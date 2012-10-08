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
import forge.Singletons;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.game.phase.ExtraTurn;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;

/**
 * <p>
 * AbilityFactory_Turns class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactoryTurns {

    // *************************************************************************
    // ************************* ADD TURN **************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityAddTurn.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityAddTurn(final AbilityFactory af) {
        class AbilityAddTurn extends AbilityActivated {
            public AbilityAddTurn(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityAddTurn(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -3526200766738015688L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryTurns.addTurnStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryTurns.addTurnCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryTurns.addTurnResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryTurns.addTurnTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abAddTurn = new AbilityAddTurn(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abAddTurn;
    }

    /**
     * <p>
     * createSpellAddTurn.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellAddTurn(final AbilityFactory af) {
        final SpellAbility spAddTurn = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -3921131887560356006L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryTurns.addTurnStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryTurns.addTurnCanPlayAI(getActivatingPlayer(), af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryTurns.addTurnResolve(af, this);
            }

            @Override
            public boolean canPlayFromEffectAI(final boolean mandatory, final boolean withOutManaCost) {
                if (withOutManaCost) {
                    return AbilityFactoryTurns.addTurnTriggerAINoCost(getActivatingPlayer(), af, this, mandatory);
                }
                return AbilityFactoryTurns.addTurnTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }

        };
        return spAddTurn;
    }

    /**
     * <p>
     * createDrawbackAddTurn.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackAddTurn(final AbilityFactory af) {
        class DrawbackAddTurn extends AbilitySub {
            public DrawbackAddTurn(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackAddTurn(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -562517287448810951L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryTurns.addTurnStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryTurns.addTurnResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryTurns.addTurnTriggerAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility dbAddTurn = new DrawbackAddTurn(af.getHostCard(), af.getAbTgt());

        return dbAddTurn;
    }

    /**
     * <p>
     * addTurnStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String addTurnStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();
        final int numTurns = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), sa);

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("takes ");
        if (numTurns > 1) {
            sb.append(numTurns);
        } else {
            sb.append("an");
        }
        sb.append(" extra turn");
        if (numTurns > 1) {
            sb.append("s");
        }
        sb.append(" after this one.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * addTurnCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean addTurnCanPlayAI(final Player ai, final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryTurns.addTurnTriggerAINoCost(ai, af, sa, false);
    }

    /**
     * <p>
     * addTurnTriggerAI.
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
    private static boolean addTurnTriggerAI(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
            return false;
        }
        return addTurnTriggerAINoCost(ai, af, sa, mandatory);
    }

    /**
     * <p>
     * addTurnTriggerAINoCost.
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
    private static boolean addTurnTriggerAINoCost(final Player ai, final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {

        final HashMap<String, String> params = af.getMapParams();

        final Player opp = ai.getOpponent();
        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai)) {
                sa.getTarget().addTarget(ai);
            } else if (mandatory && sa.canTarget(opp)) {
                sa.getTarget().addTarget(opp);
            } else {
                return false;
            }
        } else {
            final ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
            // not sure if the AI should be playing with cards that give the
            // Human more turns.
        }
        return true;
    }

    /**
     * <p>
     * addTurnResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void addTurnResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final int numTurns = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), sa);

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < numTurns; i++) {
                    ExtraTurn extra = Singletons.getModel().getGameState().getPhaseHandler().addExtraTurn(p);
                    if (params.containsKey("LoseAtEndStep")) {
                        extra.setLoseAtEndStep(true);
                    }
                    if (params.containsKey("SkipUntap")) {
                        extra.setSkipUntap(true);
                    }
                }
            }
        }
    }

    // *************************************************************************
    // ************************* END TURN **************************************
    // *************************************************************************

    /**
     * Creates the ability end turn.
     *
     * @param af the af
     * @return the spell ability
     */
    public static SpellAbility createAbilityEndTurn(final AbilityFactory af) {
        class AbilityEndTurn extends AbilityActivated {
            public AbilityEndTurn(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityEndTurn(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 72570867940224012L;

            @Override
            public String getStackDescription() {
                return "End the turn.";
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                if (mandatory) {
                    return true;
                }

                return false;
            }

            @Override
            public void resolve() {
                AbilityFactoryTurns.endTurnResolve(af, this);
            }
        }
        final SpellAbility ret = new AbilityEndTurn(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return ret;
    }

    /**
     * Creates the spell end turn.
     *
     * @param af the af
     * @return the spell ability
     */
    public static SpellAbility createSpellEndTurn(final AbilityFactory af) {
        final SpellAbility ret = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {

            private static final long serialVersionUID = -2553413143747617709L;

            @Override
            public String getStackDescription() {
                return "End the turn.";
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public void resolve() {
                AbilityFactoryTurns.endTurnResolve(af, this);
            }

        };

        return ret;
    }

    /**
     * Creates the drawback end turn.
     *
     * @param af the af
     * @return the spell ability
     */
    public static SpellAbility createDrawbackEndTurn(final AbilityFactory af) {
        class DrawbackEndTurn extends AbilitySub {
            public DrawbackEndTurn(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackEndTurn(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -562517287448810951L;

            @Override
            public String getStackDescription() {
                return "End the turn.";
            }

            @Override
            public void resolve() {
                AbilityFactoryTurns.endTurnResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                if (mandatory) {
                    return true;
                }

                return false;
            }
        }
        final SpellAbility dbEndTurn = new DrawbackEndTurn(af.getHostCard(), af.getAbTgt());

        return dbEndTurn;
    }

    private static void endTurnResolve(final AbilityFactory af, final SpellAbility sa) {

        // Steps taken from gatherer's rulings on Time Stop.
        // 1) All spells and abilities on the stack are exiled. This includes
        // Time Stop, though it will continue to resolve. It also includes
        // spells and abilities that can't be countered.
        for (final Card c : AllZone.getStackZone().getCards()) {
            Singletons.getModel().getGameAction().exile(c);
        }
        AllZone.getStack().getStack().clear();

        // 2) All attacking and blocking creatures are removed from combat.
        AllZone.getCombat().reset();

        // 3) State-based actions are checked. No player gets priority, and no
        // triggered abilities are put onto the stack.
        Singletons.getModel().getGameAction().checkStateEffects();

        // 4) The current phase and/or step ends. The game skips straight to the
        // cleanup step. The cleanup step happens in its entirety.
        Singletons.getModel().getGameState().getPhaseHandler().setPhaseState(PhaseType.CLEANUP);

        // Update observers
        AllZone.getStack().updateObservers();
        for (Player p : Singletons.getModel().getGameState().getPlayers()) {
            p.updateObservers();
            p.updateLabelObservers();
        }
    }

} // end class AbilityFactory_Turns
