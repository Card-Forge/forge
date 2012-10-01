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
package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardListUtil;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Command;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;


/**
 * <p>
 * AbilityFactory_Pump class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryPump {

    private final ArrayList<String> keywords = new ArrayList<String>();

    private String numAttack;
    private String numDefense;

    private AbilityFactory abilityFactory = null;
    private HashMap<String, String> params = null;
    private Card hostCard = null;

    /**
     * <p>
     * Constructor for AbilityFactory_Pump.
     * </p>
     * 
     * @param newAF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactoryPump(final AbilityFactory newAF) {
        this.abilityFactory = newAF;

        this.params = this.abilityFactory.getMapParams();

        this.hostCard = this.abilityFactory.getHostCard();

        this.numAttack = (this.params.containsKey("NumAtt")) ? this.params.get("NumAtt") : "0";
        this.numDefense = (this.params.containsKey("NumDef")) ? this.params.get("NumDef") : "0";

        // Start with + sign now optional
        if (this.numAttack.startsWith("+")) {
            this.numAttack = this.numAttack.substring(1);
        }
        if (this.numDefense.startsWith("+")) {
            this.numDefense = this.numDefense.substring(1);
        }

        if (this.params.containsKey("KW")) {
            final String tmp = this.params.get("KW");
            final String[] kk = tmp.split(" & ");

            this.keywords.clear();
            for (final String element : kk) {
                this.keywords.add(element);
            }
        }
    }

    /**
     * <p>
     * getSpellPump.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellPump() {
        final SpellAbility spPump = new Spell(this.hostCard, this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = 42244224L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPump.this.pumpPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPump.this.pumpStackDescription(AbilityFactoryPump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPump.this.pumpResolve(this);
            } // resolve

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryPump.this.pumpTriggerAINoCost(
                            AbilityFactoryPump.this.abilityFactory, this, mandatory);
                }
                return AbilityFactoryPump.this.pumpTriggerAI(
                        AbilityFactoryPump.this.abilityFactory, this, mandatory);
            }
        }; // SpellAbility

        return spPump;
    }

    /**
     * <p>
     * getAbilityPump.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityPump() {
        class AbilityPump extends AbilityActivated {
            public AbilityPump(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityPump(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1118592153328758083L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPump.this.pumpPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPump.this.pumpStackDescription(AbilityFactoryPump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPump.this.pumpResolve(this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPump.this.pumpTriggerAI(AbilityFactoryPump.this.abilityFactory, this, mandatory);
            }
        }
        final SpellAbility abPump = new AbilityPump(this.hostCard, this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt());

        return abPump;
    }

    /**
     * <p>
     * getDrawbackPump.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackPump() {
        class DrawbackPump extends AbilitySub {
            public DrawbackPump(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackPump(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 42244224L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPump.this.pumpPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPump.this.pumpStackDescription(AbilityFactoryPump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPump.this.pumpResolve(this);
            } // resolve

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPump.this.pumpDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPump.this.pumpTriggerAI(AbilityFactoryPump.this.abilityFactory, this, mandatory);
            }
        }
        final SpellAbility dbPump = new DrawbackPump(this.hostCard, this.abilityFactory.getAbTgt()); // SpellAbility

        return dbPump;
    }

    /**
     * <p>
     * Getter for the field <code>numAttack</code>.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    private int getNumAttack(final SpellAbility sa) {
        return AbilityFactory.calculateAmount(sa.getSourceCard(), this.numAttack, sa);
    }

    /**
     * <p>
     * Getter for the field <code>numDefense</code>.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    private int getNumDefense(final SpellAbility sa) {
        return AbilityFactory.calculateAmount(sa.getSourceCard(), this.numDefense, sa);
    }

    /**
     * Contains useful keyword.
     * 
     * @param keywords
     *            the keywords
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if successful
     */
    public boolean containsUsefulKeyword(final ArrayList<String> keywords, final Card card, final SpellAbility sa) {
        for (final String keyword : keywords) {
            if (!sa.getAbilityFactory().isCurse() && isUsefulPumpKeyword(keyword, card, sa)) {
                return true;
            }
            if (sa.getAbilityFactory().isCurse() && isUsefulCurseKeyword(keyword, card, sa)) {
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
    public boolean isUsefulCurseKeyword(final String keyword, final Card card, final SpellAbility sa) {
        final PhaseHandler ph = Singletons.getModel().getGameState().getPhaseHandler();
        final Player computer = AllZone.getComputerPlayer();
        final Player human = AllZone.getHumanPlayer();
        //int attack = getNumAttack(sa);
        //int defense = getNumDefense(sa);
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        } else if (keyword.equals("Defender") || keyword.endsWith("CARDNAME can't attack.")) {
            if (ph.isPlayerTurn(computer) || !CombatUtil.canAttack(card)
                    || (card.getNetCombatDamage() <= 0)
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can't block.")) {
            if (ph.isPlayerTurn(human) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || ph.getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }
            
            List<Card> attackers = CardListUtil.filter(AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield), CardPredicates.possibleAttackers);
            if(!CombatUtil.canBlockAtLeastOne(card, attackers)) {
                return false;
            }
        } else if (keyword.endsWith("This card doesn't untap during your next untap step.")) {
            if (ph.getPhase().isBefore(PhaseType.MAIN2) || card.isUntapped() || ph.isPlayerTurn(human)
                    || !Untap.canUntap(card)) {
                return false;
            }
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt by CARDNAME.")
                || keyword.endsWith("Prevent all damage that would be dealt by CARDNAME.")) {
            if (ph.isPlayerTurn(computer) && (!(CombatUtil.canBlock(card) || card.isBlocking())
                    || card.getNetCombatDamage() <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                    || ph.getPhase().isBefore(PhaseType.MAIN1)
                    || CardListUtil.getNotKeyword(AllZoneUtil.getCreaturesInPlay(computer), "Defender").isEmpty())) {
                return false;
            }
            if (ph.isPlayerTurn(human) && (!card.isAttacking()
                    || card.getNetCombatDamage() <= 0)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME attacks each turn if able.")) {
            if (ph.isPlayerTurn(computer) || !CombatUtil.canAttack(card) || !CombatUtil.canBeBlocked(card)
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can't be regenerated.")) {
            if (card.getShield() <= 0) {
                return false;
            }
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
    public boolean isUsefulPumpKeyword(final String keyword, final Card card, final SpellAbility sa) {
        final PhaseHandler ph = Singletons.getModel().getGameState().getPhaseHandler();
        final Player computer = AllZone.getComputerPlayer();
        final Player human = AllZone.getHumanPlayer();
        int attack = getNumAttack(sa);
        //int defense = getNumDefense(sa);
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }
        
        Predicate<Card> opBlockers = CardPredicates.possibleBlockers(card);
        List<Card> cardsCanBlock = CardListUtil.filter(AllZoneUtil.getCreaturesInPlay(human), opBlockers);
        
        final boolean evasive = (keyword.endsWith("Unblockable") || keyword.endsWith("Fear")
                || keyword.endsWith("Intimidate") || keyword.endsWith("Shadow"));
        final boolean combatRelevant = (keyword.endsWith("First Strike")
                || keyword.contains("Bushido"));
        // give evasive keywords to creatures that can or do attack
        if (evasive) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0
                    || cardsCanBlock.isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("Flying")) {
            if (ph.isPlayerTurn(human)
                    && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    && !CardListUtil.getKeyword(AllZone.getCombat().getAttackerList(), "Flying").isEmpty()
                    && !card.hasKeyword("Reach")
                    && CombatUtil.canBlock(card)
                    && CombatUtil.lifeInDanger(AllZone.getCombat())) {
                return true;
            }
            Predicate<Card> flyingOrReach = Predicates.or(CardPredicates.hasKeyword("Flying"), CardPredicates.hasKeyword("Reach"));
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0 || !Iterables.any(cardsCanBlock, Predicates.not(flyingOrReach))) {
                return false;
            }
        } else if (keyword.endsWith("Horsemanship")) {
            if (ph.isPlayerTurn(human)
                    && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    && !CardListUtil.getKeyword(AllZone.getCombat().getAttackerList(), "Horsemanship").isEmpty()
                    && CombatUtil.canBlock(card)
                    && CombatUtil.lifeInDanger(AllZone.getCombat())) {
                return true;
            }
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0
                    || CardListUtil.getNotKeyword(cardsCanBlock, "Horsemanship").isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("Haste")) {
            if (!card.hasSickness() || ph.isPlayerTurn(human) || card.isTapped()
                    || card.getNetCombatDamage() <= 0
                    || card.hasKeyword("CARDNAME can attack as though it had haste.")
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || !CombatUtil.canAttackNextTurn(card)) {
                return false;
            }
        } else if (keyword.endsWith("Indestructible")) {
            return true;
        } else if (keyword.endsWith("Deathtouch")) {
            Combat combat = AllZone.getCombat();
            if (ph.isPlayerTurn(human) && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
                List<Card> attackers = combat.getAttackers();
                for (Card attacker : attackers) {
                    if (CombatUtil.canBlock(attacker, card, combat)
                            && !CombatUtil.canDestroyAttacker(attacker, card, combat, false)) {
                        return true;
                    }
                }
            } else if (ph.isPlayerTurn(computer) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && CombatUtil.canAttack(card)) {
                List<Card> blockers = AllZoneUtil.getCreaturesInPlay(human);
                for (Card blocker : blockers) {
                    if (CombatUtil.canBlock(card, blocker, combat)
                            && !CombatUtil.canDestroyBlocker(blocker, card, combat, false)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (combatRelevant) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                    || (AllZoneUtil.getCreaturesInPlay(human).size() < 1)
                    || cardsCanBlock.isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Double Strike")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || card.getNetCombatDamage() <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                return false;
            }
        } else if (keyword.startsWith("Rampage")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || cardsCanBlock.size() < 2) {
                return false;
            }
        } else if (keyword.startsWith("Flanking")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || CardListUtil.getNotKeyword(cardsCanBlock, "Flanking").isEmpty()) {
                return false;
            }
        } else if (keyword.startsWith("Trample")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || !CombatUtil.canBeBlocked(card)
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || cardsCanBlock.isEmpty()
                    || card.getNetCombatDamage() + attack <= 1) {
                return false;
            }
        } else if (keyword.equals("Infect")) {
            if (card.getNetCombatDamage() <= 0) {
                return false;
            }
            if (card.isBlocking()) {
                return true;
            }
            if ((ph.isPlayerTurn(human))
                    || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                return false;
            }
        } else if (keyword.endsWith("Wither")) {
            if (card.getNetCombatDamage() <= 0) {
                return false;
            }
            if (card.isBlocking()) {
                return true;
            }
            if (card.isAttacking() && card.isBlocked()) {
                return true;
            }
            return false;
        } else if (keyword.equals("Lifelink")) {
            if (card.getNetCombatDamage() <= 0) {
                return false;
            }
            if (!card.isBlocking() && !card.isAttacking()) {
                return false;
            }
        } else if (keyword.equals("Vigilance")) {
            if (ph.isPlayerTurn(human) || !CombatUtil.canAttack(card)
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || CardListUtil.getNotKeyword(AllZoneUtil.getCreaturesInPlay(human), "Defender").size() < 1) {
                return false;
            }
        } else if (keyword.equals("Reach")) {
            if (ph.isPlayerTurn(computer)
                    || !ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || CardListUtil.getKeyword(AllZone.getCombat().getAttackerList(), "Flying").isEmpty()
                    || card.hasKeyword("Flying")
                    || !CombatUtil.canBlock(card)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can block an additional creature.")) {
            if (ph.isPlayerTurn(computer)
                    || !ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
                return false;
            }
            int canBlockNum = 1 + card.getKeywordAmount("CARDNAME can block an additional creature.");
            int possibleBlockNum = 0;
            for (Card attacker : AllZone.getCombat().getAttackerList()) {
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
            if (!AbilityFactory.predictThreatenedObjects(sa.getAbilityFactory()).contains(card)) {
                return false;
            }
        } else if (keyword.equals("Islandwalk")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0
                    || CardListUtil.getType(AllZoneUtil.getPlayerLandsInPlay(human), "Island").isEmpty()
                    || cardsCanBlock.isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Swampwalk")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0
                    || CardListUtil.getType(AllZoneUtil.getPlayerLandsInPlay(human), "Swamp").isEmpty()
                    || cardsCanBlock.isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Mountainwalk")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0
                    || CardListUtil.getType(AllZoneUtil.getPlayerLandsInPlay(human), "Mountain").isEmpty()
                    || cardsCanBlock.isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Forestwalk")) {
            if (ph.isPlayerTurn(human) || !(CombatUtil.canAttack(card) || card.isAttacking())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                    || card.getNetCombatDamage() <= 0
                    || CardListUtil.getType(AllZoneUtil.getPlayerLandsInPlay(human), "Forest").isEmpty()
                    || cardsCanBlock.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldPumpCard(final SpellAbility sa, final Card c) {
        int attack = getNumAttack(sa);
        int defense = getNumDefense(sa);
        PhaseHandler phase = Singletons.getModel().getGameState().getPhaseHandler();

        if (!c.canBeTargetedBy(sa)) {
            return false;
        }

        if ((c.getNetDefense() + defense) <= 0) {
            return false;
        }

        if (containsUsefulKeyword(keywords, c, sa)) {
            return true;
        }

        // will the creature attack (only relevant for sorcery speed)?
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && phase.isPlayerTurn(AllZone.getComputerPlayer())
                && attack > 0
                && CardFactoryUtil.doesCreatureAttackAI(c)) {
            return true;
        }

        // is the creature blocking and unable to destroy the attacker
        // or would be destroyed itself?
        if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY) && c.isBlocking()) {
            if (defense > 0 && CombatUtil.blockerWouldBeDestroyed(c)) {
                return true;
            }
            List<Card> blockedBy = AllZone.getCombat().getAttackersBlockedBy(c);
            // For now, Only care the first creature blocked by a card.
            // TODO Add in better BlockAdditional support
            if (!blockedBy.isEmpty() && attack > 0 && !CombatUtil.attackerWouldBeDestroyed(blockedBy.get(0))) {
                return true;
            }
        }

        // is the creature unblocked and the spell will pump its power?
        if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && AllZone.getCombat().isAttacking(c) && AllZone.getCombat().isUnblocked(c) && (attack > 0)) {
            return true;
        }

        // is the creature blocked and the blocker would survive
        if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY) && (attack > 0)
                && AllZone.getCombat().isAttacking(c) && AllZone.getCombat().isBlocked(c)
                && AllZone.getCombat().getBlockers(c) != null
                && !AllZone.getCombat().getBlockers(c).isEmpty()
                && !CombatUtil.blockerWouldBeDestroyed(AllZone.getCombat().getBlockers(c).get(0))) {
            return true;
        }

        // if the life of the computer is in danger, try to pump blockers blocking Tramplers
        List<Card> blockedBy = AllZone.getCombat().getAttackersBlockedBy(c);
        boolean attackerHasTrample = false;
        for (Card b : blockedBy) {
            attackerHasTrample |= b.hasKeyword("Trample");
        }

        if (phase.getPhase().equals(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && phase.isPlayerTurn(AllZone.getHumanPlayer())
                && c.isBlocking()
                && defense > 0
                && attackerHasTrample
                && (sa.isAbility() || CombatUtil.lifeInDanger(AllZone.getCombat()))) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * getPumpCreatures.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getPumpCreatures(final SpellAbility sa) {

        List<Card> list = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
        list = CardListUtil.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return shouldPumpCard(sa, c);
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
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param defense
     *            a int.
     * @param attack
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getCurseCreatures(final SpellAbility sa, final int defense, final int attack) {
        List<Card> list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
        list = CardListUtil.getTargetableCards(list, sa);
        if ((defense < 0) && !list.isEmpty()) { // with spells that give -X/-X,
                                                // compi will try to destroy a
                                                // creature
            list = CardListUtil.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (c.getNetDefense() <= -defense) {
                        return true; // can kill indestructible creatures
                    }
                    return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                }
            }); // leaves all creatures that will be destroyed
        } // -X/-X end
        else if ((attack < 0) && !list.isEmpty()
                && !Singletons.getModel().getGameState().getPhaseHandler().isPreventCombatDamageThisTurn()) {
            // spells that give -X/0
            Player activePlayer = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
            if (activePlayer.isComputer()) {
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
                    // TODO: Curse creatures that will block AI's creatures, if AI is going to attack.
                    list = new ArrayList<Card>();
                } else {
                    list = new ArrayList<Card>();
                }
            } else {
                // Human active, only curse attacking creatures
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    list = CardListUtil.filter(list, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (!c.isAttacking()) {
                                return false;
                            }
                            if (c.getNetAttack() > 0 && AllZone.getComputerPlayer().getLife() < 5) {
                                return true;
                            }
                            //Don't waste a -7/-0 spell on a 1/1 creature
                            if (c.getNetAttack() + attack > -2 || c.getNetAttack() > 3) {
                                return true;
                            }
                            return false;
                        }
                    });
                } else {
                    list = new ArrayList<Card>();
                }
            }
            Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_BEGIN);
        } // -X/0 end
        else if (!list.isEmpty()) {
            final ArrayList<String> keywords = this.keywords;
            final boolean addsKeywords = this.keywords.size() > 0;

            if (addsKeywords) {
                list = CardListUtil.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return containsUsefulKeyword(keywords, c, sa);
                    }
                });
            } else {
                list = new ArrayList<Card>();
            }
        }

        return list;
    } // getCurseCreatures()

    private boolean containsNonCombatKeyword(final ArrayList<String> keywords) {
        for (final String keyword : keywords) {
            // since most keywords are combat relevant check for those that are
            // not
            if (keyword.equals("HIDDEN This card doesn't untap during your next untap step.")
                    || keyword.endsWith("Shroud") || keyword.endsWith("Hexproof")) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * pumpPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpPlayAI(final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final PhaseHandler ph = Singletons.getModel().getGameState().getPhaseHandler();

        if (!CostUtil.checkLifeCost(cost, sa.getSourceCard(), 4, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(cost, sa.getSourceCard())) {
            return false;
        }

        if (!CostUtil.checkCreatureSacrificeCost(cost, sa.getSourceCard())) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, sa.getSourceCard())) {
            return false;
        }

        if (AllZone.getStack().isEmpty() && CostUtil.hasTapCost(cost, sa.getSourceCard())) {
                if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && ph.isPlayerTurn(AllZone.getComputerPlayer())) {
                    return false;
                }
                if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS) && ph.isPlayerTurn(AllZone.getHumanPlayer())) {
                    return false;
                }
        }

        final SpellAbilityRestriction restrict = sa.getRestrictions();

        // Phase Restrictions
        if ((AllZone.getStack().size() == 0) && ph.getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty
            if (!this.abilityFactory.isCurse() && !AbilityFactory.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (AllZone.getStack().size() > 0) {
            if (!this.keywords.contains("Shroud") && !this.keywords.contains("Hexproof")) {
                return false;
            }
        }

        final int activations = restrict.getNumberTurnActivations();
        final int sacActivations = restrict.getActivationNumberSacrifice();
        // don't risk sacrificing a creature just to pump it
        if ((sacActivations != -1) && (activations >= (sacActivations - 1))) {
            return false;
        }

        final Card source = sa.getSourceCard();
        if (source.getSVar("X").equals("Count$xPaid")) {
            source.setSVar("PayX", "");
        }

        int defense;
        if (this.numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
            if (this.numDefense.equals("-X")) {
                defense = -xPay;
            }
        } else {
            defense = this.getNumDefense(sa);
        }

        int attack;
        if (this.numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final String toPay = source.getSVar("PayX");

            if (toPay.equals("")) {
                final int xPay = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(toPay);
            }
        } else {
            attack = this.getNumAttack(sa);
        }

        if ((this.numDefense.contains("X") && defense == 0)
                || (this.numAttack.contains("X") && attack == 0)) {
            return false;
        }

        //Untargeted
        if ((this.abilityFactory.getAbTgt() == null) || !this.abilityFactory.getAbTgt().doesTarget()) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                    this.params.get("Defined"), sa);

            if (cards.size() == 0) {
                return false;
            }
            final Random r = MyRandom.getRandom();

            // when this happens we need to expand AI to consider if its ok for
            // everything?
            for (final Card card : cards) {
                if (this.abilityFactory.isCurse()) {
                    if (card.getController().isComputer()) {
                        return false;
                    }

                    if (!containsUsefulKeyword(this.keywords, card, sa)) {
                        continue;
                    }

                    return r.nextFloat() <= Math.pow(.9, activations);
                }
                if (shouldPumpCard(sa, card)) {
                    return r.nextFloat() <= Math.pow(.9, activations);
                }
            }
            return false;
        }
        //Targeted
        if (!this.pumpTgtAI(sa, defense, attack, false)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback()) {
            return false;
        }

        return true;
    } // pumpPlayAI()

    /**
     * <p>
     * pumpTgtAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param defense
     *            a int.
     * @param attack
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean pumpTgtAI(final SpellAbility sa, final int defense, final int attack, final boolean mandatory) {
        if (!mandatory
                && !sa.isTrigger()
                && Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && !(this.abilityFactory.isCurse() && (defense < 0))
                && !this.containsNonCombatKeyword(this.keywords)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        List<Card> list = new ArrayList<Card>();
        if (this.abilityFactory.getMapParams().containsKey("AILogic")) {
            if (this.abilityFactory.getMapParams().get("AILogic").equals("HighestPower")) {
                list = CardListUtil.getValidCards(AllZoneUtil.getCreaturesInPlay(), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
                list = CardListUtil.getTargetableCards(list, sa);
                CardListUtil.sortAttack(list);
                if (!list.isEmpty()) {
                    tgt.addTarget(list.get(0));
                    return true;
                } else {
                    return false;
                }
            }
        } else if (this.abilityFactory.isCurse()) {
            if (sa.canTarget(AllZone.getHumanPlayer())) {
                tgt.addTarget(AllZone.getHumanPlayer());
                return true;
            }
            list = this.getCurseCreatures(sa, defense, attack);
        } else {
            if (!tgt.canTgtCreature()) {
                ZoneType zone = tgt.getZone().get(0);
                list = AllZoneUtil.getCardsIn(zone);
            } else {
                list = this.getPumpCreatures(sa);
            }
            if (sa.canTarget(AllZone.getComputerPlayer())) {
                tgt.addTarget(AllZone.getComputerPlayer());
                return true;
            }
        }

        list = CardListUtil.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
        if (AllZone.getStack().size() == 0) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().getTap()) {
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getComputerPlayer())) {
                    list.remove(sa.getSourceCard());
                }
                if (Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(AllZone.getHumanPlayer())) {
                    list.remove(sa.getSourceCard());
                }
            }
        }

        if (list.isEmpty()) {
            return mandatory && this.pumpMandatoryTarget(this.abilityFactory, sa, mandatory);
        }

        if (!this.abilityFactory.isCurse()) {
            // Don't target cards that will die.
            list = CardListUtil.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.getSVar("Targeting").equals("Dies");
                }
            });
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return this.pumpMandatoryTarget(this.abilityFactory, sa, mandatory);
                    }

                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = CardFactoryUtil.getBestAI(list);
            tgt.addTarget(t);
            list.remove(t);
        }

        return true;
    } // pumpTgtAI()

    /**
     * <p>
     * pumpMandatoryTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean pumpMandatoryTarget(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        List<Card> list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Target tgt = sa.getTarget();
        list = CardListUtil.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
        list = CardListUtil.getTargetableCards(list, sa);

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        List<Card> pref;
        List<Card> forced;
        final Card source = sa.getSourceCard();

        if (af.isCurse()) {
            pref = CardListUtil.filterControlledBy(list, AllZone.getHumanPlayer());
            forced = CardListUtil.filterControlledBy(list, AllZone.getComputerPlayer());
        } else {
            pref = CardListUtil.filterControlledBy(list, AllZone.getComputerPlayer());
            forced = CardListUtil.filterControlledBy(list, AllZone.getHumanPlayer());
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (CardListUtil.getNotType(pref, "Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref, sa, true);
            }

            pref.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (CardListUtil.getNotType(forced, "Creature").size() == 0) {
                c = CardFactoryUtil.getWorstCreatureAI(forced);
            } else {
                c = CardFactoryUtil.getCheapestPermanentAI(forced, sa, true);
            }

            forced.remove(c);

            tgt.addTarget(c);
        }

        if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    /**
     * <p>
     * pumpTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean pumpTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }
        return pumpTriggerAINoCost(af, sa, mandatory);
    }

    /**
     * <p>
     * pumpTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean pumpTriggerAINoCost(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getSourceCard();

        int defense;
        if (this.numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
        } else {
            defense = this.getNumDefense(sa);
        }

        int attack;
        if (this.numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final String toPay = source.getSVar("PayX");

            if (toPay.equals("")) {
                final int xPay = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(toPay);
            }
        } else {
            attack = this.getNumAttack(sa);
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return this.pumpTgtAI(sa, defense, attack, mandatory);
        }

        return true;
    } // pumpTriggerAI

    /**
     * <p>
     * pumpDrawbackAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpDrawbackAI(final SpellAbility sa) {
        final Card source = sa.getSourceCard();
        int defense;
        if (this.numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            defense = Integer.parseInt(source.getSVar("PayX"));
        } else {
            defense = this.getNumDefense(sa);
        }

        int attack;
        if (this.numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            attack = Integer.parseInt(source.getSVar("PayX"));
        } else {
            attack = this.getNumAttack(sa);
        }

        if ((this.abilityFactory.getAbTgt() == null) || !this.abilityFactory.getAbTgt().doesTarget()) {
            if (source.isCreature()) {
                if (!source.hasKeyword("Indestructible")
                        && ((source.getNetDefense() + defense) <= source.getDamage())) {
                    return false;
                }
                if ((source.getNetDefense() + defense) <= 0) {
                    return false;
                }
            }
        } else {
            //Targeted
            if (!this.pumpTgtAI(sa, defense, attack, false)) {
                return false;
            }

            final AbilitySub subAb = sa.getSubAbility();
            if (subAb != null && !subAb.chkAIDrawback()) {
                return false;
            }
        }

        return true;
    } // pumpDrawbackAI()

    /**
     * <p>
     * pumpStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String pumpStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // when damageStackDescription is called, just build exactly what is
        // happening
        final StringBuilder sb = new StringBuilder();
        ArrayList<GameEntity> tgts = new ArrayList<GameEntity>();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgts.addAll(tgt.getTargetCards());
            tgts.addAll(tgt.getTargetPlayers());
        } else {
            if (params.containsKey("Defined")) {
                tgts.addAll(AbilityFactory.getDefinedPlayers(sa.getSourceCard(), this.params.get("Defined"), sa));
            }
            if (tgts.isEmpty()) {
                tgts.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), this.params.get("Defined"), sa));
            }
        }

        if (tgts.size() > 0) {

            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(af.getHostCard()).append(" - ");
            }

            if (params.containsKey("StackDescription")) {
                if (params.get("StackDescription").equals("None")) {
                    sb.append("");
                } else {
                sb.append(params.get("StackDescription"));
                }
            }

            else {
                for (final GameEntity c : tgts) {
                    sb.append(c).append(" ");
                }

                if (af.getMapParams().containsKey("Radiance")) {
                    sb.append(" and each other ").append(af.getMapParams().get("ValidTgts"))
                            .append(" that shares a color with ");
                    if (tgts.size() > 1) {
                        sb.append("them ");
                    } else {
                        sb.append("it ");
                    }
                }

                final int atk = this.getNumAttack(sa);
                final int def = this.getNumDefense(sa);

                sb.append("gains ");
                if ((atk != 0) || (def != 0)) {
                    if (atk >= 0) {
                        sb.append("+");
                    }
                    sb.append(atk);
                    sb.append("/");
                    if (def >= 0) {
                       sb.append("+");
                    }
                    sb.append(def);
                    sb.append(" ");
                }

                for (int i = 0; i < this.keywords.size(); i++) {
                    sb.append(this.keywords.get(i)).append(" ");
                }

                if (!this.params.containsKey("Permanent")) {
                    sb.append("until end of turn.");
                }
            }
        }

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // pumpStackDescription()

    /**
     * <p>
     * pumpResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void pumpResolve(final SpellAbility sa) {
        ArrayList<Card> tgtCards = new ArrayList<Card>();
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        final Target tgt = sa.getTarget();
        ArrayList<Player> tgtPlayers = new ArrayList<Player>();
        String pumpRemembered = null;

        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            if (params.containsKey("Defined")) {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), this.params.get("Defined"), sa);
            }
            if (tgtPlayers.isEmpty()) {
                tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), this.params.get("Defined"), sa);
            }
        }

        if (this.params.containsKey("Optional")) {
            if (sa.getActivatingPlayer().isHuman()) {
                final StringBuilder targets = new StringBuilder();
                for (final Card tc : tgtCards) {
                    targets.append(tc);
                }
                final StringBuilder sb = new StringBuilder();
                final String descBasic = "Apply pump to " + targets + "?";
                final String pumpDesc = params.containsKey("OptionQuestion")
                        ? params.get("OptionQuestion").replace("TARGETS", targets) : descBasic;
                sb.append(pumpDesc);
                if (!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) {
                   return;
                }
            } else { //Computer player
                //TODO Add logic here if necessary but I think the AI won't cast
                //the spell in the first place if it would curse its own creature
                //and the pump isn't mandatory
            }
        }

        if (this.params.containsKey("RememberObjects")) {
            pumpRemembered = params.get("RememberObjects");
        }

        if (pumpRemembered != null) {
            for (final Object o : AbilityFactory.getDefinedObjects(sa.getSourceCard(), pumpRemembered, sa)) {
                if (!sa.getSourceCard().getRemembered().contains(o)) {
                    sa.getSourceCard().addRemembered(o);
                }
            }
        }

        if (this.params.containsKey("Radiance")) {
            for (final Card c : CardUtil.getRadiance(sa.getSourceCard(), tgtCards.get(0), this.params.get("ValidTgts")
                    .split(","))) {
                untargetedCards.add(c);
            }
        }

        final ZoneType pumpZone = this.params.containsKey("PumpZone") ? ZoneType.smartValueOf(this.params.get("PumpZone"))
                : ZoneType.Battlefield;

        final int size = tgtCards.size();
        for (int j = 0; j < size; j++) {
            final Card tgtC = tgtCards.get(j);

            // only pump things in PumpZone
            if (!AllZoneUtil.getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            // if pump is a target, make sure we can still target now
            if ((tgt != null) && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            this.applyPump(sa, tgtC);
        }

        for (int i = 0; i < untargetedCards.size(); i++) {
            final Card tgtC = untargetedCards.get(i);
            // only pump things in PumpZone
            if (!AllZoneUtil.getCardsIn(pumpZone).contains(tgtC)) {
                continue;
            }

            this.applyPump(sa, tgtC);
        }

        for (Player p : tgtPlayers) {
            if (!p.canBeTargetedBy(sa)) {
                continue;
            }

            this.applyPump(sa, p);
        }
    } // pumpResolve()

    private void applyPump(final SpellAbility sa, final Card applyTo) {

        //if host is not on the battlefield don't apply
        if (this.params.containsKey("UntilLoseControlOfHost")
                && !AllZoneUtil.isCardInPlay(sa.getSourceCard())) {
            return;
        }
        final int a = this.getNumAttack(sa);
        final int d = this.getNumDefense(sa);

        applyTo.addTempAttackBoost(a);
        applyTo.addTempDefenseBoost(d);

        for (int i = 0; i < this.keywords.size(); i++) {
            applyTo.addExtrinsicKeyword(this.keywords.get(i));
            if (this.keywords.get(i).equals("Suspend")) {
                applyTo.setSuspend(true);
            }
        }

        if (!this.params.containsKey("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -42244224L;

                @Override
                public void execute() {
                    applyTo.addTempAttackBoost(-1 * a);
                    applyTo.addTempDefenseBoost(-1 * d);

                    if (AbilityFactoryPump.this.keywords.size() > 0) {
                        for (int i = 0; i < AbilityFactoryPump.this.keywords.size(); i++) {
                            applyTo.removeExtrinsicKeyword(AbilityFactoryPump.this.keywords.get(i));
                        }
                    }
                }
            };
            if (this.params.containsKey("UntilEndOfCombat")) {
                AllZone.getEndOfCombat().addUntil(untilEOT);
            } else if (this.params.containsKey("UntilYourNextUpkeep")) {
                Singletons.getModel().getGameState().getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else if (params.containsKey("UntilHostLeavesPlay")) {
                sa.getSourceCard().addLeavesPlayCommand(untilEOT);
            } else if (this.params.containsKey("UntilLoseControlOfHost")) {
                sa.getSourceCard().addLeavesPlayCommand(untilEOT);
                sa.getSourceCard().addChangeControllerCommand(untilEOT);
            } else if (this.params.containsKey("UntilYourNextTurn")) {
                Singletons.getModel().getGameState().getCleanup().addUntilYourNextTurn(sa.getActivatingPlayer(), untilEOT);
            } else {
                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    private void applyPump(final SpellAbility sa, final Player p) {

        for (int i = 0; i < this.keywords.size(); i++) {
            p.addKeyword(this.keywords.get(i));
        }

        if (!this.params.containsKey("Permanent")) {
            // If not Permanent, remove Pumped at EOT
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -32453460L;

                @Override
                public void execute() {

                    if (AbilityFactoryPump.this.keywords.size() > 0) {
                        for (int i = 0; i < AbilityFactoryPump.this.keywords.size(); i++) {
                            p.removeKeyword(AbilityFactoryPump.this.keywords.get(i));
                        }
                    }
                }
            };
            if (this.params.containsKey("UntilEndOfCombat")) {
                Singletons.getModel().getGameState().getEndOfCombat().addUntil(untilEOT);
            } else if (this.params.containsKey("UntilYourNextUpkeep")) {
                Singletons.getModel().getGameState().getUpkeep().addUntil(sa.getActivatingPlayer(), untilEOT);
            } else {
                Singletons.getModel().getGameState().getEndOfTurn().addUntil(untilEOT);
            }
        }
    }

    // ///////////////////////////////////
    //
    // PumpAll
    //
    // ////////////////////////////////////

    /**
     * <p>
     * getAbilityPumpAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbilityPumpAll() {
        class AbilityPumpAll extends AbilityActivated {
            public AbilityPumpAll(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityPumpAll(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -8299417521903307630L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPump.this.pumpAllCanPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPump.this.pumpAllStackDescription(AbilityFactoryPump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPump.this.pumpAllResolve(this);
            } // resolve()

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPump.this
                        .pumpAllTriggerAI(AbilityFactoryPump.this.abilityFactory, this, mandatory);
            }
        }
        final SpellAbility abPumpAll = new AbilityPumpAll(this.hostCard, this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt());

        return abPumpAll;
    }

    /**
     * <p>
     * getSpellPumpAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellPumpAll() {
        final SpellAbility spPumpAll = new Spell(this.hostCard, this.abilityFactory.getAbCost(),
                this.abilityFactory.getAbTgt()) {
            private static final long serialVersionUID = -4055467978660824703L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPump.this.pumpAllCanPlayAI(this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryPump.this.pumpAllStackDescription(AbilityFactoryPump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPump.this.pumpAllResolve(this);
            } // resolve
        }; // SpellAbility

        return spPumpAll;
    }

    /**
     * <p>
     * getDrawbackPumpAll.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getDrawbackPumpAll() {
        class DrawbackPumpAll extends AbilitySub {
            public DrawbackPumpAll(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackPumpAll(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 6411531984691660342L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryPump.this.pumpAllStackDescription(AbilityFactoryPump.this.abilityFactory, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryPump.this.pumpAllResolve(this);
            } // resolve

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryPump.this.pumpAllCanPlayAI(this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryPump.this.pumpAllChkDrawbackAI(this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryPump.this
                        .pumpAllTriggerAI(AbilityFactoryPump.this.abilityFactory, this, mandatory);
            }
        }
        final SpellAbility dbPumpAll = new DrawbackPumpAll(this.hostCard, this.abilityFactory.getAbTgt()); // SpellAbility

        return dbPumpAll;
    }

    /**
     * <p>
     * pumpAllCanPlayAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpAllCanPlayAI(final SpellAbility sa) {
        String valid = "";
        final Random r = MyRandom.getRandom();
        final Card source = sa.getSourceCard();
        this.params = this.abilityFactory.getMapParams();
        final int power = this.getNumAttack(sa);
        final int defense = this.getNumDefense(sa);
        final PhaseType phase = Singletons.getModel().getGameState().getPhaseHandler().getPhase();

        // prevent runaway activations
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn()); // to

        if (this.params.containsKey("ValidCards")) {
            valid = this.params.get("ValidCards");
        }

        List<Card> comp = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
        comp = CardListUtil.getValidCards(comp, valid, source.getController(), source);
        List<Card> human = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        human = CardListUtil.getValidCards(human, valid, source.getController(), source);

        final Target tgt = sa.getTarget();
        if (tgt != null && sa.canTarget(AllZone.getHumanPlayer()) && params.containsKey("IsCurse")) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getHumanPlayer());
            comp = new ArrayList<Card>();
        }

        if (this.abilityFactory.isCurse()) {
            if (defense < 0) { // try to destroy creatures
                comp = CardListUtil.filter(comp, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.getNetDefense() <= -defense) {
                            return true; // can kill indestructible creatures
                        }
                        return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                    }
                }); // leaves all creatures that will be destroyed
                human = CardListUtil.filter(human, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.getNetDefense() <= -defense) {
                            return true; // can kill indestructible creatures
                        }
                        return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                    }
                }); // leaves all creatures that will be destroyed
            } // -X/-X end

            // evaluate both lists and pass only if human creatures are more
            // valuable
            if ((CardFactoryUtil.evaluateCreatureList(comp) + 200) >= CardFactoryUtil.evaluateCreatureList(human)) {
                return false;
            }
            return chance;
        } // end Curse

        // don't use non curse PumpAll after Combat_Begin until AI is improved
        if (phase.isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
            return false;
        }

        // only count creatures that can attack
        comp = CardListUtil.filter(comp, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (power <= 0 && !containsUsefulKeyword(keywords, c, sa)) {
                    return false;
                }
                if (phase.equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) && c.isAttacking()) {
                    return true;
                }
                if (phase.isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && CombatUtil.canAttack(c)) {
                    return true;
                }
                return false;
            }
        });

        if ((comp.size() <= human.size()) || (comp.size() <= 1)) {
            return false;
        }

        return chance;
    } // pumpAllCanPlayAI()

    /**
     * <p>
     * pumpAllResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void pumpAllResolve(final SpellAbility sa) {
        List<Card> list;
        ArrayList<Player> tgtPlayers = null;
        final ArrayList<ZoneType> affectedZones = new ArrayList<ZoneType>();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (this.params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), this.params.get("Defined"), sa);
        }

        if (this.params.containsKey("PumpZone")) {
            for (final String zone : this.params.get("PumpZone").split(",")) {
                affectedZones.add(ZoneType.valueOf(zone));
            }
        } else {
            affectedZones.add(ZoneType.Battlefield);
        }

        list = new ArrayList<Card>();
        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            for (final ZoneType zone : affectedZones) {
                list.addAll(AllZoneUtil.getCardsIn(zone));
            }

        } else {
            for (final ZoneType zone : affectedZones) {
                list.addAll(tgtPlayers.get(0).getCardsIn(zone));
            }
        }

        String valid = "";
        if (this.params.containsKey("ValidCards")) {
            valid = this.params.get("ValidCards");
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        final int a = this.getNumAttack(sa);
        final int d = this.getNumDefense(sa);

        for (final Card c : list) {
            final Card tgtC = c;

            // only pump things in the affected zones.
            boolean found = false;
            for (final ZoneType z : affectedZones) {
                if (c.isInZone(z)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                continue;
            }

            tgtC.addTempAttackBoost(a);
            tgtC.addTempDefenseBoost(d);

            for (int i = 0; i < this.keywords.size(); i++) {
                tgtC.addExtrinsicKeyword(this.keywords.get(i));
            }

            if (!this.params.containsKey("Permanent")) {
                // If not Permanent, remove Pumped at EOT
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = 5415795460189457660L;

                    @Override
                    public void execute() {
                        if (AllZoneUtil.isCardInPlay(tgtC)) {
                            tgtC.addTempAttackBoost(-1 * a);
                            tgtC.addTempDefenseBoost(-1 * d);

                            if (AbilityFactoryPump.this.keywords.size() > 0) {
                                for (int i = 0; i < AbilityFactoryPump.this.keywords.size(); i++) {
                                    tgtC.removeExtrinsicKeyword(AbilityFactoryPump.this.keywords.get(i));
                                }
                            }
                        }
                    }
                };

                AllZone.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // pumpAllResolve()

    /**
     * <p>
     * pumpAllTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean pumpAllTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        // TODO: add targeting consideration such as
        // "Creatures target player controls gets"

        return true;
    }

    /**
     * <p>
     * pumpAllChkDrawbackAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean pumpAllChkDrawbackAI(final SpellAbility sa) {
        return true;
    }

    /**
     * <p>
     * pumpAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String pumpAllStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (this.params.containsKey("SpellDescription")) {
            desc = this.params.get("SpellDescription");
        } else if (this.params.containsKey("PumpAllDescription")) {
            desc = this.params.get("PumpAllDescription");
        }

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
        sb.append(desc);

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    } // pumpAllStackDescription()

} // end class AbilityFactory_Pump
