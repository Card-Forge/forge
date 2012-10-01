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

import com.google.common.base.Predicate;

import forge.AllZone;
import forge.Card;

import forge.CardListUtil;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.zone.PlayerZone;
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
     * worshipFlag.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean worshipFlag(final Player player) {
        // Instead of hardcoded Ali from Cairo like cards, it is now a Keyword
        List<Card> list = player.getCardsIn(ZoneType.Battlefield);
        list = CardListUtil.getKeyword(list, "Damage that would reduce your life total to less than 1 reduces it to 1 instead.");
        list = CardListUtil.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.isFaceDown();
            }
        });

        return list.size() > 0;
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
                if (AllZone.getHumanPlayer().getZone(ZoneType.Hand).size() == 0) {
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
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(ZoneType.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    if (card.isType(uType.toString())) {
                        this.stop();
                    } else {
                        if ((this.n == nCards) || (AllZone.getHumanPlayer().getZone(ZoneType.Hand).size() == 0)) {
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
                if (AllZone.getHumanPlayer().getZone(ZoneType.Hand).size() == 0) {
                    this.stop();
                }
                if (nCards == 0) {
                    this.stop();
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage("Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(ZoneType.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    // in case no more cards in hand
                    if ((this.n == nCards) || (AllZone.getHumanPlayer().getZone(ZoneType.Hand).size() == 0)) {
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
     * input_chainsDiscard.
     * </p>
     * 
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputChainsDiscard() {
        final Input target = new Input() {
            private static final long serialVersionUID = 2856894846224546303L;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(ZoneType.Hand).size() == 0) {
                    this.stop();
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage("Chains of Mephistopheles:\n" + "Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(ZoneType.Hand)) {
                    card.getController().discard(card, null);
                    this.done();
                }
            }

            private void done() {
                this.stop();
                // hack to not trigger Chains of Mephistopheles recursively
                AllZone.getHumanPlayer().drawCards(1, true);
            }
        };
        return target;
    } // input_chainsDiscard()

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
     * @return a {@link forge.control.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanents(final int nCards) {
        final List<Card> list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
        return PlayerUtil.inputSacrificePermanentsFromList(nCards, list, "Select a permanent to sacrifice");
    } // input_sacrificePermanents()

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
        List<Card> list = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);

        list = CardListUtil.getType(list, type);
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
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.equals(AllZone.getHumanPlayer().getZone(ZoneType.Battlefield)) && list.contains(card)) {
                    Singletons.getModel().getGameAction().sacrifice(card, null);
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
