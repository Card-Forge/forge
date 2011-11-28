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

import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;

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
        final SpellAbility abCharm = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4038591081733095021L;

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryCharm.charmCanPlayAI(af, this);
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
                return AbilityFactoryCharm.charmCanPlayAI(af, this);
            }
        }; // Ability_Activated

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
                return AbilityFactoryCharm.charmCanPlayAI(af, this);
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

    private static boolean charmCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // TODO - enable Charms for the AI
        return false;
    }

    private static void charmResolve(final AbilityFactory af, final SpellAbility sa) {
        // nothing to do. Ability_Subs are set up in
        // GameAction.playSpellAbility(),
        // and that Ability_Sub.resolve() is called from AbilityFactory
    }

    /**
     * Sets the up charm s as.
     * 
     * @param sa
     *            the new up charm s as
     */
    public static void setupCharmSAs(final SpellAbility sa) {
        // make Charm choices
        if (sa.isCharm()) {
            final ArrayList<SpellAbility> choices = new ArrayList<SpellAbility>();
            choices.addAll(sa.getCharmChoices());
            for (int i = 0; i < choices.size(); i++) {
                if (!sa.canPlay()) {
                    choices.remove(sa);
                }
            }
            for (int i = 0; i < sa.getCharmNumber(); i++) {
                Object o;
                if (i < sa.getMinCharmNumber()) {
                    o = GuiUtils.getChoice("Choose a mode", choices.toArray());
                } else {
                    o = GuiUtils.getChoiceOptional("Choose a mode", choices.toArray());
                }
                if (null == o) {
                    break;
                }
                final AbilitySub chosen = (AbilitySub) o;
                sa.addCharmChoice(chosen);
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
        }
    }

} // end class AbilityFactory_Charm
