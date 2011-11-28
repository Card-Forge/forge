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

import forge.Constant.Zone;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

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
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public static boolean worshipFlag(final Player player) {
        // Instead of hardcoded Ali from Cairo like cards, it is now a Keyword
        CardList list = player.getCardsIn(Zone.Battlefield);
        list = list.getKeyword("Damage that would reduce your life total to less than 1 reduces it to 1 instead.");
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
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
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputDiscardNumUnless(final int nCards, final String uType, final SpellAbility sa) {
        final SpellAbility sp = sa;
        final Input target = new Input() {
            private static final long serialVersionUID = 8822292413831640944L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    this.stop();
                }
                AllZone.getDisplay().showMessage(
                        "Select " + (nCards - this.n) + " cards to discard, unless you discard a " + uType + ".");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    if (card.isType(uType.toString())) {
                        this.stop();
                    } else {
                        if ((this.n == nCards) || (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0)) {
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
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputDiscard(final int nCards, final SpellAbility sa) {
        final SpellAbility sp = sa;
        final Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    this.stop();
                }
                if (nCards == 0) {
                    this.stop();
                }

                AllZone.getDisplay().showMessage("Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    card.getController().discard(card, sp);
                    this.n++;

                    // in case no more cards in hand
                    if ((this.n == nCards) || (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0)) {
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
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputChainsDiscard() {
        final Input target = new Input() {
            private static final long serialVersionUID = 2856894846224546303L;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    this.stop();
                }

                AllZone.getDisplay().showMessage("Chains of Mephistopheles:\n" + "Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
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
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanent(final CardList choices, final String message) {
        return PlayerUtil.inputSacrificePermanentsFromList(1, choices, message);
    } // input_sacrifice()

    /**
     * <p>
     * input_sacrificePermanents.
     * </p>
     * 
     * @param nCards
     *            a int.
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanents(final int nCards) {
        final CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        list.remove("Mana Pool"); // is this needed?
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
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanents(final int nCards, final String type) {
        CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        list.remove("Mana Pool"); // is this needed?

        list = list.getType(type);
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
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputSacrificePermanentsFromList(final int nCards, final CardList list, final String message) {
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

                AllZone.getDisplay().showMessage(message + " (" + (nCards - this.n) + " left)");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.equals(AllZone.getHumanPlayer().getZone(Zone.Battlefield)) && list.contains(card)) {
                    AllZone.getGameAction().sacrifice(card);
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

    /**
     * <p>
     * input_putFromHandToLibrary.
     * </p>
     * 
     * @param topOrBottom
     *            a {@link java.lang.String} object.
     * @param num
     *            a int.
     * @return a {@link forge.gui.input.Input} object.
     * @since 1.0.15
     */
    public static Input inputPutFromHandToLibrary(final String topOrBottom, final int num) {
        final Input target = new Input() {
            private static final long serialVersionUID = 5178077952030689103L;
            private int n = 0;

            @Override
            public void showMessage() {
                AllZone.getDisplay().showMessage("Select a card to put on the " + topOrBottom + " of your library.");
                ButtonUtil.disableAll();

                if ((this.n == num) || (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0)) {
                    this.stop();
                }
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    int position = 0;
                    if (topOrBottom.equalsIgnoreCase("bottom")) {
                        position = -1;
                    }

                    AllZone.getGameAction().moveToLibrary(card, position);

                    this.n++;
                    if (this.n == num) {
                        this.stop();
                    }

                    this.showMessage();
                }
            }
        };
        return target;
    }

} // end class PlayerUtil
