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

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cost.Cost;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory_Combat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryCombat {

    private AbilityFactoryCombat() {
        throw new AssertionError();
    }

    // **************************************************************
    // ****************************** FOG **************************
    // **************************************************************

    /**
     * <p>
     * createAbilityFog.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityFog(final AbilityFactory af) {
        class AbilityFog extends AbilityActivated {
            public AbilityFog(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityFog(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -1933592438783630254L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryCombat.fogStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.fogCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.fogResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.fogDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abFog = new AbilityFog(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abFog;
    }

    /**
     * <p>
     * createSpellFog.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellFog(final AbilityFactory af) {
        final SpellAbility spFog = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryCombat.fogStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.fogCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.fogResolve(af, this);
            }

        };
        return spFog;
    }

    /**
     * <p>
     * createDrawbackFog.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackFog(final AbilityFactory af) {
        class DrawbackFog extends AbilitySub {
            public DrawbackFog(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackFog(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -5141246507533353605L;

            @Override
            public void resolve() {
                AbilityFactoryCombat.fogResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCombat.fogPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.fogDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbFog = new DrawbackFog(af.getHostCard(), af.getAbTgt());

        return dbFog;
    }

    /**
     * <p>
     * fogStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String fogStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        sb.append(sa.getSourceCard().getController());
        sb.append(" prevents all combat damage this turn.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * fogCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean fogCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's Declare Blockers phase
        if (Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
            return false;
        }
        if (!Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        // Only cast when Stack is empty, so Human uses spells/abilities first
        if (AllZone.getStack().size() != 0) {
            return false;
        }

        // Don't cast it, if the effect is already in place
        if (Singletons.getModel().getGameState().getPhaseHandler().isPreventCombatDamageThisTurn()) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            if (!subAb.chkAIDrawback()) {
                return false;
            }
        }

        // Cast it if life is in danger
        return CombatUtil.lifeInDanger(AllZone.getCombat());
    }

    /**
     * <p>
     * fogPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean fogPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance;
        if (Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer().getOpponent())) {
            chance = Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    /**
     * <p>
     * fogDoTriggerAI.
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
    public static boolean fogDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        boolean chance;
        if (Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer().getOpponent())) {
            chance = Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        } else {
            chance = Singletons.getModel().getGameState().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE);
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return chance && abSub.doTrigger(mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * fogResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void fogResolve(final AbilityFactory af, final SpellAbility sa) {

        // Expand Fog keyword here depending on what we need out of it.
        Singletons.getModel().getGameState().getPhaseHandler().setPreventCombatDamageThisTurn(true);
    }

    // **************************************************************
    // *********************** MUSTATTACK ***************************
    // **************************************************************

    // AB$ MustAttack | Cost$ R T | ValidTgts$ Opponent | TgtPrompt$ Select
    // target opponent | Defender$ Self | SpellDescription$ ...

    /**
     * <p>
     * createAbilityMustAttack.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.1.01
     */
    public static SpellAbility createAbilityMustAttack(final AbilityFactory af) {
        class AbilityMustAttack extends AbilityActivated {
            public AbilityMustAttack(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityMustAttack(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4559154732470225755L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCombat.mustAttackStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.mustAttackCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.mustAttackResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.mustAttackDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abMustAttack = new AbilityMustAttack(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abMustAttack;
    }

    /**
     * <p>
     * createSpellMustAttack.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellMustAttack(final AbilityFactory af) {
        final SpellAbility spMustAttack = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4103945257601008403L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCombat.mustAttackStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.mustAttackCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.mustAttackResolve(af, this);
            }

        };
        return spMustAttack;
    }

    /**
     * <p>
     * createDrawbackMustAttack.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackMustAttack(final AbilityFactory af) {
        class DrawbackMustAttack extends AbilitySub {
            public DrawbackMustAttack(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackMustAttack(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 1294949210616598158L;

            @Override
            public void resolve() {
                AbilityFactoryCombat.mustAttackResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCombat.mustAttackPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.mustAttackDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbMustAttack = new DrawbackMustAttack(af.getHostCard(), af.getAbTgt());

        return dbMustAttack;
    }

    private static String mustAttackStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        // end standard pre-

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String defender = null;
        if (params.get("Defender").equals("Self")) {
            defender = host.toString();
        } else {
            // TODO - if more needs arise in the future
        }

        for (final Player player : tgtPlayers) {
            sb.append("Creatures ").append(player).append(" controls attack ");
            sb.append(defender).append(" during his or her next turn.");
        }

        // begin standard post-
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean mustAttackCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // disabled for the AI for now. Only for Gideon Jura at this time.
        return false;
    }

    private static boolean mustAttackPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance;

        // TODO - implement AI
        chance = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    private static boolean mustAttackDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        boolean chance;

        // TODO - implement AI
        chance = false;

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return chance && abSub.doTrigger(mandatory);
        }

        return chance;
    }

    private static void mustAttackResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                Object entity;
                if (params.get("Defender").equals("Self")) {
                    entity = sa.getSourceCard();
                } else {
                    entity = p.getOpponent();
                }
                // System.out.println("Setting mustAttackEntity to: "+entity);
                p.setMustAttackEntity(entity);
            }
        }

    } // mustAttackResolve()

    // **************************************************************
    // ********************* RemoveFromCombat ***********************
    // **************************************************************

    /**
     * <p>
     * createAbilityRemoveFromCombat.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.1.6
     */
    public static SpellAbility createAbilityRemoveFromCombat(final AbilityFactory af) {
        class AbilityRemoveFromCombat extends AbilityActivated {
            public AbilityRemoveFromCombat(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityRemoveFromCombat(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -2472319390656924874L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCombat.removeFromCombatStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.removeFromCombatCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.removeFromCombatResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.removeFromCombatDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abRemCombat = new AbilityRemoveFromCombat(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abRemCombat;
    }

    /**
     * <p>
     * createSpellRemoveFeomCombat.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellRemoveFromCombat(final AbilityFactory af) {
        final SpellAbility spRemCombat = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4086879057558760897L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCombat.removeFromCombatStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.removeFromCombatCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.removeFromCombatResolve(af, this);
            }

        };
        return spRemCombat;
    }

    /**
     * <p>
     * createDrawbackRemoveFromCombat.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRemoveFromCombat(final AbilityFactory af) {
        class DrawbackRemoveFromCombat extends AbilitySub {
            public DrawbackRemoveFromCombat(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackRemoveFromCombat(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 5080737903616292224L;

            @Override
            public void resolve() {
                AbilityFactoryCombat.removeFromCombatResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCombat.removeFromCombatPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.removeFromCombatDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbRemCombat = new DrawbackRemoveFromCombat(af.getHostCard(), af.getAbTgt());

        return dbRemCombat;
    }

    private static String removeFromCombatStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        // end standard pre-

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        sb.append("Remove ");

        for (final Card c : tgtCards) {
            sb.append(c);
        }

        sb.append(" from combat.");

        // begin standard post-
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean removeFromCombatCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // disabled for the AI for now. Only for Gideon Jura at this time.
        return false;
    }

    private static boolean removeFromCombatPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance;

        // TODO - implement AI
        chance = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    private static boolean removeFromCombatDoTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory) {
        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        boolean chance;

        // TODO - implement AI
        chance = false;

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return chance && abSub.doTrigger(mandatory);
        }

        return chance;
    }

    private static void removeFromCombatResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                AllZone.getCombat().removeFromCombat(c);
            }
        }

    } // mustAttackResolve()

    // **************************************************************
    // *********************** MustBlock ****************************
    // **************************************************************

    // AB$ MustBlock | Cost$ R T | ValidTgts$ Creature.YouDontCtrl | TgtPrompt$
    // Select target creature defending player controls | DefinedAttacker$ Self
    // | SpellDescription$ ...

    /**
     * <p>
     * createAbilityMustBlock.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.1.6
     */
    public static SpellAbility createAbilityMustBlock(final AbilityFactory af) {
        class AbilityMustBlock extends AbilityActivated {
            public AbilityMustBlock(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityMustBlock(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 4237190949098526123L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCombat.mustBlockStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.mustBlockCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.mustBlockResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.mustBlockDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility abMustBlock = new AbilityMustBlock(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abMustBlock;
    }

    /**
     * <p>
     * createSpellMustBlock.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.1.6
     */
    public static SpellAbility createSpellMustBlock(final AbilityFactory af) {
        final SpellAbility spMustBlock = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6758785067306305860L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryCombat.mustBlockStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCombat.mustBlockCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryCombat.mustBlockResolve(af, this);
            }

        };
        return spMustBlock;
    }

    /**
     * <p>
     * createDrawbackMustBlock.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * 
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackMustBlock(final AbilityFactory af) {
        class DrawbackMustBlock extends AbilitySub {
            public DrawbackMustBlock(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackMustBlock(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -815813765448972775L;

            @Override
            public void resolve() {
                AbilityFactoryCombat.mustBlockResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return AbilityFactoryCombat.mustBlockPlayDrawbackAI(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCombat.mustBlockDoTriggerAI(af, this, mandatory);
            }
        }
        final SpellAbility dbMustBlock = new DrawbackMustBlock(af.getHostCard(), af.getAbTgt());

        return dbMustBlock;
    }

    private static String mustBlockStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        // end standard pre-

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String attacker = null;
        if (params.containsKey("DefinedAttacker")) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                    params.get("DefinedAttacker"), sa);
            attacker = cards.get(0).toString();
        } else {
            attacker = host.toString();
        }

        for (final Card c : tgtCards) {
            sb.append(c).append(" must block ").append(attacker).append(" if able.");
        }

        // begin standard post-
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean mustBlockCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // disabled for the AI until he/she can make decisions about who to make
        // block
        return false;
    }

    private static boolean mustBlockPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // AI should only activate this during Human's turn
        boolean chance;

        // TODO - implement AI
        chance = false;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    private static boolean mustBlockDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        final HashMap<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        final Target abTgt = sa.getTarget();

        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        // only use on creatures that can attack
        if (!Singletons.getModel().getGameState().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        }

        Card attacker = null;
        if (params.containsKey("DefinedAttacker")) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                    params.get("DefinedAttacker"), sa);
            if (cards.isEmpty()) {
                return false;
            }

            attacker = cards.get(0);
        }

        if (attacker == null) {
            attacker = source;
        }

        final Card definedAttacker = attacker;

        boolean chance = false;

        if (abTgt != null) {
            CardList list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield).getType("Creature");
            list = list.getTargetableCards(sa);
            list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    boolean tapped = c.isTapped();
                    c.setTapped(false);
                    if (!CombatUtil.canBlock(definedAttacker, c)) {
                        return false;
                    }
                    if (CombatUtil.canDestroyAttacker(definedAttacker, c, null, false)) {
                        return false;
                    }
                    if (!CombatUtil.canDestroyBlocker(c, definedAttacker, null, false)) {
                        return false;
                    }
                    c.setTapped(tapped);
                    return true;
                }
            });
            if (list.isEmpty()) {
                return false;
            }
            final Card blocker = CardFactoryUtil.getBestCreatureAI(list);
            if (blocker == null) {
                return false;
            }
            abTgt.addTarget(blocker);
            chance = true;
        } else {
            return false;
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return chance && abSub.doTrigger(mandatory);
        }

        return chance;
    }

    private static void mustBlockResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = sa.getSourceCard();

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        ArrayList<Card> cards;
        if (params.containsKey("DefinedAttacker")) {
            cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("DefinedAttacker"), sa);
        } else {
            cards = new ArrayList<Card>();
            cards.add(host);
        }

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                final Card attacker = cards.get(0);
                c.addMustBlockCard(attacker);
                System.out.println(c + " is adding " + attacker + " to mustBlockCards: " + c.getMustBlockCards());
            }
        }

    } // mustBlockResolve()

} // end class AbilityFactory_Combat
