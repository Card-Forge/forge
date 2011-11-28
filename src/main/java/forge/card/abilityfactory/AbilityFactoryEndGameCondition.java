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

import forge.AllZone;
import forge.Card;
import forge.ComputerUtil;
import forge.Player;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameLossReason;

/**
 * <p>
 * AbilityFactory_EndGameCondition class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryEndGameCondition {

    private AbilityFactoryEndGameCondition() {
        throw new AssertionError();
    }

    // ***********************************************************************************************
    // ***************************************** Wins Game
    // *******************************************
    // ***********************************************************************************************
    /**
     * <p>
     * createAbilityWinsGame.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityWinsGame(final AbilityFactory af) {

        final SpellAbility abWinsGame = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 8869422603616247307L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryEndGameCondition.winsGameStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryEndGameCondition.winsGameCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryEndGameCondition.winsGameResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryEndGameCondition.winsGameDoTriggerAI(af, this, mandatory);
            }

        };
        return abWinsGame;
    }

    /**
     * <p>
     * createSpellWinsGame.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellWinsGame(final AbilityFactory af) {
        final SpellAbility spWinsGame = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryEndGameCondition.winsGameStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryEndGameCondition.winsGameCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryEndGameCondition.winsGameResolve(af, this);
            }

        };
        return spWinsGame;
    }

    /**
     * <p>
     * createDrawbackWinsGame.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackWinsGame(final AbilityFactory af) {
        final SpellAbility dbWinsGame = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryEndGameCondition.winsGameStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryEndGameCondition.winsGameCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryEndGameCondition.winsGameResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryEndGameCondition.winsGameDoTriggerAI(af, this, mandatory);
            }

        };
        return dbWinsGame;
    }

    /**
     * <p>
     * winsGameStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String winsGameStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        // Let the spell description also be the stack description
        sb.append(sa.getDescription());

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * winsGameCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean winsGameCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        if (AllZone.getComputerPlayer().cantWin()) {
            return false;
        }

        // TODO Check conditions are met on card (e.g. Coalition Victory)

        // TODO Consider likelihood of SA getting countered

        // In general, don't return true.
        // But this card wins the game, I can make an exception for that
        return true;
    }

    /**
     * <p>
     * winsGameDoTriggerAI.
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
    public static boolean winsGameDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        // WinGame abilities usually don't have subAbilities but for
        // consistency...
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * winsGameResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void winsGameResolve(final AbilityFactory af, final SpellAbility sa) {

        final Card card = af.getHostCard();

        final ArrayList<Player> players = AbilityFactory.getDefinedPlayers(card, af.getMapParams().get("Defined"), sa);

        for (final Player p : players) {
            p.altWinBySpellEffect(card.getName());
        }
    }

    // ***********************************************************************************************
    // **************************************** Loses Game
    // *******************************************
    // ***********************************************************************************************

    /**
     * <p>
     * createAbilityLosesGame.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityLosesGame(final AbilityFactory af) {

        final SpellAbility abLosesGame = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 8869422603616247307L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryEndGameCondition.losesGameStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryEndGameCondition.losesGameCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryEndGameCondition.losesGameResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryEndGameCondition.losesGameDoTriggerAI(af, this, mandatory);
            }

        };
        return abLosesGame;
    }

    /**
     * <p>
     * createSpellLosesGame.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellLosesGame(final AbilityFactory af) {
        final SpellAbility spLosesGame = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryEndGameCondition.losesGameStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryEndGameCondition.losesGameCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryEndGameCondition.losesGameResolve(af, this);
            }

        };
        return spLosesGame;
    }

    /**
     * <p>
     * createDrawbackLosesGame.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackLosesGame(final AbilityFactory af) {
        final SpellAbility dbLosesGame = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return AbilityFactoryEndGameCondition.losesGameStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                // if X depends on abCost, the AI needs to choose which card he
                // would sacrifice first
                // then call xCount with that card to properly calculate the
                // amount
                // Or choosing how many to sacrifice
                return AbilityFactoryEndGameCondition.losesGameCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryEndGameCondition.losesGameResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryEndGameCondition.losesGameDoTriggerAI(af, this, mandatory);
            }

        };
        return dbLosesGame;
    }

    /**
     * <p>
     * losesGameStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String losesGameStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card source = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(source.getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        final Target tgt = sa.getTarget();
        ArrayList<Player> players = null;
        if (sa.getTarget() != null) {
            players = tgt.getTargetPlayers();
        } else {
            players = AbilityFactory.getDefinedPlayers(source, af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : players) {
            sb.append(p.getName()).append(" ");
        }

        sb.append("loses the game.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * losesGameCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean losesGameCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        if (AllZone.getHumanPlayer().cantLose()) {
            return false;
        }

        // Only one SA Lose the Game card right now, which is Door to
        // Nothingness

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            tgt.addTarget(AllZone.getHumanPlayer());
        }

        // In general, don't return true.
        // But this card wins the game, I can make an exception for that
        return true;
    }

    /**
     * <p>
     * losesGameDoTriggerAI.
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
    public static boolean losesGameDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // If there is a cost payment it's usually not mandatory
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            return false;
        }

        // Phage the Untouchable
        // (Final Fortune would need to attach it's delayed trigger to a
        // specific turn, which can't be done yet)

        if (!mandatory && AllZone.getHumanPlayer().cantLose()) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            tgt.addTarget(AllZone.getHumanPlayer());
        }

        // WinGame abilities usually don't have subAbilities but for
        // consistency...
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * losesGameResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void losesGameResolve(final AbilityFactory af, final SpellAbility sa) {

        final Card card = af.getHostCard();

        final Target tgt = sa.getTarget();
        ArrayList<Player> players = null;
        if (sa.getTarget() != null) {
            players = tgt.getTargetPlayers();
        } else {
            players = AbilityFactory.getDefinedPlayers(card, af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : players) {
            p.loseConditionMet(GameLossReason.SpellEffect, card.getName());
        }
    }

} // end class AbilityFactory_EndGameCondition
