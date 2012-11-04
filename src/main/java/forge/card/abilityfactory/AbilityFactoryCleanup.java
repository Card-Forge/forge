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

import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

// Cleanup is not the same as other AFs, it is only used as a Drawback, and only used to Cleanup particular card states
// That need to be reset. I'm creating this to clear Remembered Cards at the
// end of an Effect so they don't get shown on a card
// After the effect finishes resolving.
/**
 * <p>
 * AbilityFactory_Cleanup class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryCleanup {

    private AbilityFactoryCleanup() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getDrawback.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    public static AbilitySub getDrawback(final AbilityFactory af) {
        class DrawbackCleanup extends AbilitySub {
            public DrawbackCleanup(final Card ca, final Target t) {
                super(ca, t);
            }

            @Override
            public AbilitySub getCopy() {
                AbilitySub res = new DrawbackCleanup(getSourceCard(),
                        getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                return res;
            }

            private static final long serialVersionUID = 6192972525033429820L;

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public void resolve() {
                AbilityFactoryCleanup.doResolve(af, this);
            }
        }
        final AbilitySub drawback = new DrawbackCleanup(af.getHostCard(), af.getAbTgt());

        return drawback;
    }

    /**
     * <p>
     * doResolve.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void doResolve(final AbilityFactory af, final SpellAbility sa) {
        final Map<String, String> params = af.getMapParams();
        Card source = sa.getSourceCard();

        if (params.containsKey("ClearRemembered")) {
            source.clearRemembered();
            Singletons.getModel().getGame().getCardState(source).clearRemembered();
        }
        if (params.containsKey("ClearImprinted")) {
            source.clearImprinted();
        }
        if (params.containsKey("ClearChosenX")) {
            source.setSVar("ChosenX", "");
        }
        if (params.containsKey("ClearChosenY")) {
            source.setSVar("ChosenY", "");
        }
        if (params.containsKey("ClearTriggered")) {
            Singletons.getModel().getGame().getTriggerHandler().clearDelayedTrigger(source);
        }
    }

} // end class AbilityFactory_Cleanup
