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
package forge.game.player;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * <p>
 * PlayerUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class PlayerUtil {

    private PlayerUtil() {
        throw new AssertionError();
    }

    /**
     * <p>
     * input_discardNumUnless.
     * </p>
     * 
     * @param nCards
     *            a int.
     * @param uType
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.control.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputDiscardNumUnless(final int nCards, final String uType, final SpellAbility sa) {
        final SpellAbility sp = sa;
        final Input target = new Input() {
            private static final long serialVersionUID = 8822292413831640944L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (Singletons.getControl().getPlayer().getZone(ZoneType.Hand).size() == 0) {
                    this.stop();
                }
                CMatchUI.SINGLETON_INSTANCE.showMessage(
                        "Select " + (nCards - this.n) + " cards to discard, unless you discard a " + uType + ".");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                if (zone.is(ZoneType.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    if (card.isType(uType.toString())) {
                        this.stop();
                    } else {
                        if ((this.n == nCards) || (Singletons.getControl().getPlayer().getZone(ZoneType.Hand).size() == 0)) {
                            this.stop();
                        } else {
                            this.showMessage();
                        }
                    }
                }
            }
        };

        return target;
    } // input_discardNumUnless

    /**
     * <p>
     * input_discard.
     * </p>
     * 
     * @param nCards
     *            a int.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.control.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputDiscard(final int nCards, final SpellAbility sa) {
        final SpellAbility sp = sa;
        final Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (Singletons.getControl().getPlayer().getZone(ZoneType.Hand).size() == 0) {
                    this.stop();
                }
                if (nCards == 0) {
                    this.stop();
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage("Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                if (zone.is(ZoneType.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    // in case no more cards in hand
                    if ((this.n == nCards) || (Singletons.getControl().getPlayer().getZone(ZoneType.Hand).size() == 0)) {
                        this.stop();
                    } else {
                        this.showMessage();
                    }
                }
            }
        };
        return target;
    } // input_discard()

    /**
     * <p>
     * input_sacrificePermanent.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanent(final List<Card> choices, final String message) {
        return PlayerUtil.inputSacrificePermanentsFromList(1, choices, message);
    } // input_sacrifice()

    /**
     * <p>
     * input_sacrificePermanents.
     * </p>
     * 
     * @param nCards
     *            a int.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanents(final int nCards, final String type) {
        final List<Card> list = CardLists.getType(Singletons.getControl().getPlayer().getCardsIn(ZoneType.Battlefield), type);

        return PlayerUtil.inputSacrificePermanentsFromList(nCards, list, "Select a " + type + " to sacrifice");
    } // input_sacrificePermanents()

    /**
     * <p>
     * input_sacrificePermanentsFromList.
     * </p>
     * 
     * @param nCards
     *            a int.
     * @param list
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanentsFromList(final int nCards, final List<Card> list, final String message) {
        final Input target = new Input() {
            private static final long serialVersionUID = 1981791992623774490L;
            private int n = 0;

            @Override
            public void showMessage() {
                // in case no more {type}s in play
                if ((this.n == nCards) || (list.size() == 0)) {
                    this.stop();
                    return;
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage(message + " (" + (nCards - this.n) + " left)");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                if (zone.equals(Singletons.getControl().getPlayer().getZone(ZoneType.Battlefield)) && list.contains(card)) {
                    Singletons.getModel().getGame().getAction().sacrifice(card, null);
                    this.n++;
                    list.remove(card);

                    // in case no more {type}s in play
                    if ((this.n == nCards) || (list.size() == 0)) {
                        this.stop();
                        return;
                    } else {
                        this.showMessage();
                    }
                }
            }
        };
        return target;
    } // input_sacrificePermanents()

} // end class PlayerUtil
