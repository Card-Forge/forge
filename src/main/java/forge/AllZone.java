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
package forge;

import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryInterface;
import forge.game.limited.GauntletMini;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


/**
 * Please use public getters and setters instead of direct field access.
 * <p/>
 * If you need a setter, by all means, add it.
 * 
 * @author Forge
 * @version $Id$
 */
public final class AllZone {
    // only for testing, should read decks from local directory
    // public static final IO IO = new IO("all-decks");

    /**
     * Do not instantiate.
     */
    private AllZone() {
        // blank
    }

    /** Global <code>gauntletData</code>. */
    private static forge.game.limited.GauntletMini gauntlet = null;

    /** Constant <code>COLOR_CHANGER</code>. */
    private static final ColorChanger COLOR_CHANGER = new ColorChanger();

    // Phase is now a prerequisite for CardFactory
    /** Global <code>cardFactory</code>. */
    private static CardFactoryInterface cardFactory = null;

    // initialized at Runtime since it has to be the last object constructed

    // shared between Input_Attack, Input_Block, Input_CombatDamage ,
    // InputState_Computer

    /**
     * <p>
     * getGauntletData.
     * </p>
     * 
     * @return a {@link forge.quest.data.QuestData} object.
     * @since 1.0.15
     */
    public static forge.game.limited.GauntletMini getGauntlet() {

        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return AllZone.gauntlet;
    }

    /**
     * <p>
     * getCardFactory.
     * </p>
     * 
     * @return a {@link forge.card.cardfactory.CardFactoryInterface} object.
     * @since 1.0.15
     */
    public static CardFactoryInterface getCardFactory() {
        if (AllZone.cardFactory == null) {
            AllZone.setCardFactory(new CardFactory(ForgeProps.getFile(NewConstants.CARDSFOLDER)));
        }
        return AllZone.cardFactory;
    }

    /**
     * Setter for cardFactory.
     * 
     * @param factory
     *            the factory to set
     */
    private static void setCardFactory(final CardFactoryInterface factory) {
        AllZone.cardFactory = factory;
    }

    /**
     * Create and return the next timestamp.
     * 
     * Will eventually be marked deprecated.
     * 
     * @return the next timestamp
     */
    public static long getNextTimestamp() {
        return Singletons.getModel().getGameState().getNextTimestamp();
    }

    /**
     * Getter for colorChanger.
     * 
     * @return the colorChanger
     */
    public static ColorChanger getColorChanger() {
        return AllZone.COLOR_CHANGER;
    }


} // AllZone
