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
import java.util.Map;
import java.util.Random;

import forge.Card;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.game.player.Player;
import forge.gui.GuiChoose;
import forge.util.MyRandom;

// Charm specific params:
// Choices - a ","-delimited list of SVars containing ability choices

/**
 * <p>
 * AbilityFactory_Charm class.
 * </p>
 * 
 * @author Forge
 */
public final class AbilityFactoryCharm {

    private AbilityFactoryCharm() {
        throw new AssertionError();
    }

    /**
     * <p>
     * createAbilityCharm.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityCharm(final AbilityFactory af) {
        class AbilityCharm extends AbilityActivated {
            public AbilityCharm(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityCharm(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = -4038591081733095021L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCharm.charmCanPlayAI(getActivatingPlayer(), af, this, false);
            }

            @Override
            public void resolve() {
                AbilityFactoryCharm.charmResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryCharm.charmStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryCharm.charmCanPlayAI(getActivatingPlayer(), af, this, mandatory);
            }
        }
        final SpellAbility abCharm = new AbilityCharm(af.getHostCard(), af.getAbCost(), af.getAbTgt());

        return abCharm;
    }

    /**
     * <p>
     * createSpellCharm.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellCharm(final AbilityFactory af) {
        final SpellAbility spCharm = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7297235470289087240L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCharm.charmCanPlayAI(getActivatingPlayer(), af, this, false);
            }

            @Override
            public void resolve() {
                AbilityFactoryCharm.charmResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryCharm.charmStackDescription(af, this);
            }
        };

        return spCharm;
    }

    private static String charmStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
        // end standard begin

        // nothing stack specific for Charm

        // begin standard post
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean charmCanPlayAI(final Player ai,final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
        final Random r = MyRandom.getRandom();
        final Map<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        //this resets all previous choices
        sa.setSubAbility(null);
        final int num = Integer.parseInt(params.containsKey("CharmNum") ? params.get("CharmNum")
                : "1");
        final String[] saChoices = params.get("Choices").split(",");
        ArrayList<SpellAbility> choices = new ArrayList<SpellAbility>();
        for (final String saChoice : saChoices) {
            final String ab = source.getSVar(saChoice);
            final AbilityFactory charmAF = new AbilityFactory();
            choices.add(charmAF.getAbility(ab, source));
        }
        boolean timingRight = false; //is there a reason to play the charm now?
        if (sa.isTrigger()) {
            timingRight = true;
        }
        for (int i = 0; i < num; i++) {
            AbilitySub chosen = null;
            for (SpellAbility sub : choices) {
                sub.setActivatingPlayer(ai);
                if (!timingRight && sub.canPlayAI()) {
                    chosen = (AbilitySub) sub;
                    choices.remove(sub);
                    timingRight = true;
                    break;
                }
                if ((timingRight || i < num - 1) && sub.doTrigger(false)) {
                    chosen = (AbilitySub) sub;
                    choices.remove(sub);
                    break;
                }
            }
            if (chosen == null) {
                return false;
            }

            // walk down the SpellAbility tree and add to the child
            // Ability_Sub
            SpellAbility child = sa;
            while (child.getSubAbility() != null) {
                child = child.getSubAbility();
            }
            child.setSubAbility(chosen);
            if (chosen.getActivatingPlayer() == null) {
                chosen.setActivatingPlayer(child.getActivatingPlayer());
            }
            chosen.setParent(child);
        }
        if (!timingRight) {
            return false;
        }
        // prevent run-away activations - first time will always return true
        return r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
    }

    private static void charmResolve(final AbilityFactory af, final SpellAbility sa) {
        // nothing to do. Ability_Subs are set up in
        // GameAction.playSpellAbility(),
        // and that Ability_Sub.resolve() is called from AbilityFactory
    }

    /**
     * Setup charm. For HUMAN only
     * 
     * @param sa
     *            the new up charm s as
     */
    public static SpellAbility setupCharmSAs(final SpellAbility sa) {
        AbilityFactory af = sa.getAbilityFactory();
        if (af == null || af.getAPI() != ApiType.Charm || sa.isWrapper()) {
            return sa;
        }
        final Map<String, String> params = af.getMapParams();
        final Card source = sa.getSourceCard();
        //this resets all previous choices
        sa.setSubAbility(null);
        final int num = Integer.parseInt(params.containsKey("CharmNum") ? params.get("CharmNum")
                : "1");
        final int min = params.containsKey("MinCharmNum") ? Integer.parseInt(params.get("MinCharmNum")) : num;
        final String[] saChoices = params.get("Choices").split(",");
        ArrayList<SpellAbility> choices = new ArrayList<SpellAbility>();
        for (final String saChoice : saChoices) {
            final String ab = source.getSVar(saChoice);
            final AbilityFactory charmAF = new AbilityFactory();
            choices.add(charmAF.getAbility(ab, source));
        }

        for (int i = 0; i < num; i++) {
            Object o;
            if (i < min) {
                o = GuiChoose.one("Choose a mode", choices);
            } else {
                o = GuiChoose.oneOrNone("Choose a mode", choices);
            }
            if (null == o) {
                return sa;
            }
            final AbilitySub chosen = (AbilitySub) o;
            choices.remove(chosen);

            // walk down the SpellAbility tree and add to the child
            // Ability_Sub
            SpellAbility child = sa;
            while (child.getSubAbility() != null) {
                child = child.getSubAbility();
            }
            child.setSubAbility(chosen);
            if (chosen.getActivatingPlayer() == null) {
                chosen.setActivatingPlayer(child.getActivatingPlayer());
            }
            chosen.setParent(child);
        }
        return sa;
    }

} // end class AbilityFactory_Charm
