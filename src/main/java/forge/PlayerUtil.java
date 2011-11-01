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
        Input target = new Input() {
            private static final long serialVersionUID = 8822292413831640944L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    stop();
                }
                AllZone.getDisplay().showMessage(
                        "Select " + (nCards - n) + " cards to discard, unless you discard a " + uType + ".");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectButtonCancel() {
                stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    card.getController().discard(card, sp);
                    n++;

                    if (card.isType(uType.toString())) {
                        stop();
                    } else {
                        if (n == nCards || AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                            stop();
                        } else {
                            showMessage();
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
        Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;

            private int n = 0;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    stop();
                }
                if (nCards == 0) {
                    stop();
                }

                AllZone.getDisplay().showMessage("Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    card.getController().discard(card, sp);
                    n++;

                    // in case no more cards in hand
                    if (n == nCards || AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                        stop();
                    } else {
                        showMessage();
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
        Input target = new Input() {
            private static final long serialVersionUID = 2856894846224546303L;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    stop();
                }

                AllZone.getDisplay().showMessage("Chains of Mephistopheles:\n" + "Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    card.getController().discard(card, null);
                    done();
                }
            }

            private void done() {
                stop();
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
        return inputSacrificePermanentsFromList(1, choices, message);
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
        CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        list.remove("Mana Pool"); // is this needed?
        return inputSacrificePermanentsFromList(nCards, list, "Select a permanent to sacrifice");
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
        return inputSacrificePermanentsFromList(nCards, list, "Select a " + type + " to sacrifice");
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
        Input target = new Input() {
            private static final long serialVersionUID = 1981791992623774490L;
            private int n = 0;

            @Override
            public void showMessage() {
                // in case no more {type}s in play
                if (n == nCards || list.size() == 0) {
                    stop();
                    return;
                }

                AllZone.getDisplay().showMessage(message + " (" + (nCards - n) + " left)");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.equals(AllZone.getHumanPlayer().getZone(Zone.Battlefield)) && list.contains(card)) {
                    AllZone.getGameAction().sacrifice(card);
                    n++;
                    list.remove(card);

                    // in case no more {type}s in play
                    if (n == nCards || list.size() == 0) {
                        stop();
                        return;
                    } else {
                        showMessage();
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
        Input target = new Input() {
            private static final long serialVersionUID = 5178077952030689103L;
            private int n = 0;

            @Override
            public void showMessage() {
                AllZone.getDisplay().showMessage("Select a card to put on the " + topOrBottom + " of your library.");
                ButtonUtil.disableAll();

                if (n == num || AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    stop();
                }
            }

            @Override
            public void selectButtonCancel() {
                stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    int position = 0;
                    if (topOrBottom.equalsIgnoreCase("bottom")) {
                        position = -1;
                    }

                    AllZone.getGameAction().moveToLibrary(card, position);

                    n++;
                    if (n == num) {
                        stop();
                    }

                    showMessage();
                }
            }
        };
        return target;
    }

} // end class PlayerUtil
