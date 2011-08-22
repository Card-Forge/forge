package forge.card.cost;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCostUtil;

public class Cost_Input {
    // ******************************************************************************
    // *********** Inputs used by Cost_Payment below here ***************************
    // ******************************************************************************

    /**
     * <p>input_payMana.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param manaToAdd a int.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_payMana(final SpellAbility sa, final Cost_Payment payment, final CostMana costMana, final int manaToAdd) {
        final ManaCost manaCost;

        if (Phase.getGameBegins() == 1) {
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {
                String mana = costMana.getMana();
                manaCost = new ManaCost(mana);
                manaCost.increaseColorlessMana(manaToAdd);
            }
        } else {
            System.out.println("Is input_payMana ever called when the Game isn't in progress?");
            manaCost = new ManaCost(sa.getManaCost());
        }

        Input payMana = new Input() {
            private ManaCost mana = manaCost;
            private static final long serialVersionUID = 3467312982164195091L;

            private final String originalManaCost = costMana.getMana();

            private int phyLifeToLose = 0;

            private void resetManaCost() {
                mana = new ManaCost(originalManaCost);
                phyLifeToLose = 0;
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                // prevent cards from tapping themselves if ability is a tapability, although it should already be tapped
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    return;
                }

                mana = Input_PayManaCostUtil.activateManaAbility(sa, card, mana);

                if (mana.isPaid())
                    done();
                else if (AllZone.getInputControl().getInput() == this)
                    showMessage();
            }

            @Override
            public void selectPlayer(Player player) {
                if (player.isHuman()) {
                    if (manaCost.payPhyrexian()) {
                        phyLifeToLose += 2;
                    }

                    showMessage();
                }
            }

            private void done() {
                Card source = sa.getSourceCard();
                if (phyLifeToLose > 0)
                    AllZone.getHumanPlayer().payLife(phyLifeToLose, source);
                source.setColorsPaid(mana.getColorsPaid());
                source.setSunburstValue(mana.getSunburst());
                resetManaCost();
                stop();
                
                if (costMana.hasNoXManaCost() || manaToAdd > 0){
                    payment.paidCost(costMana);
                }
                else{
                    source.setXManaCostPaid(0);
                    CostUtil.setInput(input_payXMana(sa, payment, costMana, costMana.getXMana()));
                }
                    
            }

            @Override
            public void selectButtonCancel() {
                stop();
                resetManaCost();
                payment.cancelCost();
                AllZone.getHumanBattlefield().updateObservers();
            }

            @Override
            public void showMessage() {
                ButtonUtil.enableOnlyCancel();
                String displayMana = mana.toString().replace("X", "").trim();
                AllZone.getDisplay().showMessage("Pay Mana Cost: " + displayMana);

                StringBuilder msg = new StringBuilder("Pay Mana Cost: " + displayMana);
                if (phyLifeToLose > 0) {
                    msg.append(" (");
                    msg.append(phyLifeToLose);
                    msg.append(" life paid for phyrexian mana)");
                }

                if (mana.containsPhyrexianMana()) {
                    msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
                }

                AllZone.getDisplay().showMessage(msg.toString());
                if (mana.isPaid())
                    done();
            }
        };
        return payMana;
    }

    /**
     * <p>input_payXMana.</p>
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param costMana TODO
     * @param numX a int.
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_payXMana(final SpellAbility sa, final Cost_Payment payment, final CostMana costMana, final int numX) {
        Input payX = new Input() {
            private static final long serialVersionUID = -6900234444347364050L;
            int xPaid = 0;
            ManaCost manaCost = new ManaCost(Integer.toString(numX));

            @Override
            public void showMessage() {
                if (manaCost.toString().equals(Integer.toString(numX))) // Can only cancel if partially paid an X value
                    ButtonUtil.enableAll();
                else
                    ButtonUtil.enableOnlyCancel();

                AllZone.getDisplay().showMessage("Pay X Mana Cost for " + sa.getSourceCard().getName() + "\n" + xPaid + " Paid so far.");
            }

            // selectCard
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    // this really shouldn't happen but just in case
                    return;
                }

                manaCost = Input_PayManaCostUtil.activateManaAbility(sa, card, manaCost);
                if (manaCost.isPaid()) {
                    manaCost = new ManaCost(Integer.toString(numX));
                    xPaid++;
                }

                if (AllZone.getInputControl().getInput() == this)
                    showMessage();
            }

            @Override
            public void selectButtonCancel() {
                stop();
                payment.cancelCost();
                AllZone.getHumanBattlefield().updateObservers();
            }

            @Override
            public void selectButtonOK() {
                stop();
                payment.getCard().setXManaCostPaid(xPaid);
                payment.paidCost(costMana);
            }

        };

        return payX;
    }

    /**
     * <p>input_discardCost.</p>
     * @param discType a {@link java.lang.String} object.
     * @param handList a {@link forge.CardList} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @param nNeeded a int.
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_discardCost(final String discType, final CardList handList, SpellAbility sa, final Cost_Payment payment, final CostPart part, final int nNeeded) {
        final SpellAbility sp = sa;
        Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;

            int nDiscard = 0;

            @Override
            public void showMessage() {
                boolean any = discType.equals("Any") ? true : false;
                if (AllZone.getHumanHand().size() == 0) stop();
                StringBuilder type = new StringBuilder("");
                if (any || !discType.equals("Card")) {
                    type.append(" ").append(discType);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Select ");
                if (any) {
                    sb.append("any ");
                } else {
                    sb.append("a ").append(type.toString()).append(" ");
                }
                sb.append("card to discard.");
                if (nNeeded > 1) {
                    sb.append(" You have ");
                    sb.append(nNeeded - nDiscard);
                    sb.append(" remaining.");
                }
                AllZone.getDisplay().showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand) && handList.contains(card)) {
                    // send in CardList for Typing
                    card.getController().discard(card, sp);
                    handList.remove(card);
                    nDiscard++;

                    //in case no more cards in hand
                    if (nDiscard == nNeeded)
                        done();
                    else if (AllZone.getHumanHand().size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }

            public void done() {
                stop();
                payment.paidCost(part);
            }
        };

        return target;
    }//input_discard() 

    /**
     * <p>sacrificeThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeThis(final SpellAbility sa, final Cost_Payment payment, final CostPart part) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;

            @Override
            public void showMessage() {
                Card card = sa.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlay(card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Sacrifice?");
                    Object[] possibleValues = {"Yes", "No"};
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, possibleValues, possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.getAbility().addCostToHashList(card, "Sacrificed");
                        AllZone.getGameAction().sacrifice(card);
                        stop();
                        payment.paidCost(part);
                    } else {
                        stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    }//input_sacrifice()

    /**
     * <p>sacrificeType.</p>
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @param typeList TODO
     * @param num TODO
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeFromList(final SpellAbility sa, final Cost_Payment payment, final CostSacrifice part, final CardList typeList, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private int nSacrifices = 0;

            @Override
            public void showMessage() {
                StringBuilder msg = new StringBuilder("Sacrifice ");
                int nLeft = nNeeded - nSacrifices;
                msg.append(nLeft).append(" ");
                msg.append(part.getDescriptiveType());
                if (nLeft > 1) {
                    msg.append("s");
                }

                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (typeList.contains(card)) {
                    nSacrifices++;
                    payment.getAbility().addCostToHashList(card, "Sacrificed");
                    AllZone.getGameAction().sacrifice(card);
                    typeList.remove(card);
                    //in case nothing else to sacrifice
                    if (nSacrifices == nNeeded)
                        done();
                    else if (typeList.size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }

            public void done() {
                stop();
                payment.paidCost(part);
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }
        };

        return target;
    }//sacrificeType()

    /**
     * <p>sacrificeAllType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @param typeList TODO
     */
    public static void sacrificeAll(final SpellAbility sa, final Cost_Payment payment, CostPart part, CardList typeList) {
        // TODO Ask First
        for (Card card : typeList) {
            payment.getAbility().addCostToHashList(card, "Sacrificed");
            AllZone.getGameAction().sacrifice(card);
        }

        payment.setPaidManaPart(part, true);
    }

    /**
     * <p>exileThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param costExile TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileThis(final SpellAbility sa, final Cost_Payment payment, final CostExile part) {
        Input target = new Input() {
            private static final long serialVersionUID = 678668673002725001L;

            @Override
            public void showMessage() {
                Card card = sa.getSourceCard();
                if (sa.getActivatingPlayer().isHuman() && AllZoneUtil.isCardInZone(AllZone.getZone(part.getFrom(), sa.getActivatingPlayer()), card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Exile?");
                    Object[] possibleValues = {"Yes", "No"};
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, possibleValues, possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        AllZone.getGameAction().exile(card);
                        stop();
                        payment.paidCost(part);
                    } else {
                        stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    }//input_exile()


    /**
     * <p>exileType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param costExile TODO
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileType(final SpellAbility sa, final CostExile part, final String type, final Cost_Payment payment, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 1403915758082824694L;

            private CardList typeList;
            private int nExiles = 0;

            @Override
            public void showMessage() {
                StringBuilder msg = new StringBuilder("Exile ");
                int nLeft = nNeeded - nExiles;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }

                typeList = AllZoneUtil.getPlayerCardsInPlay(sa.getSourceCard().getController());
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (typeList.contains(card)) {
                    nExiles++;
                    payment.getAbility().addCostToHashList(card, "Exiled");
                    AllZone.getGameAction().exile(card);
                    typeList.remove(card);
                    //in case nothing else to exile
                    if (nExiles == nNeeded)
                        done();
                    else if (typeList.size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }

            public void done() {
                stop();
                payment.paidCost(part);
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }
        };

        return target;
    }//exileType()

    /**
     * <p>input_tapXCost.</p>
     *
     * @param nCards a int.
     * @param cardType a {@link java.lang.String} object.
     * @param cardList a {@link forge.CardList} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_tapXCost(final CostTapType tapType, final CardList cardList, SpellAbility sa, final Cost_Payment payment, final int nCards) {      
        Input target = new Input() {

            private static final long serialVersionUID = 6438988130447851042L;
            int nTapped = 0;

            @Override
            public void showMessage() {
                if (cardList.size() == 0) stop();

                int left = nCards - nTapped;
                AllZone.getDisplay().showMessage("Select a " + tapType.getDescription() + " to tap (" + left + " left)");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (zone.is(Constant.Zone.Battlefield) && cardList.contains(card) && card.isUntapped()) {
                    // send in CardList for Typing
                    card.tap();
                    tapType.addToList(card);
                    cardList.remove(card);
                    payment.getAbility().addCostToHashList(card, "Tapped");
                    nTapped++;

                    if (nTapped == nCards)
                        done();
                    else if (cardList.size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }

            public void done() {
                stop();
                payment.paidCost(tapType);
            }
        };

        return target;
    }//input_tapXCost() 

    /**
     * <p>returnThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input returnThis(final SpellAbility sa, final Cost_Payment payment, final CostPart part) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;

            @Override
            public void showMessage() {
                Card card = sa.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlay(card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Return to Hand?");
                    Object[] possibleValues = {"Yes", "No"};
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, possibleValues, possibleValues[0]);
                    if (choice.equals(0)) {
                        AllZone.getGameAction().moveToHand(card);
                        stop();
                        payment.paidCost(part);
                    } else {
                        stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    }//input_sacrifice()
    

    /**
     * <p>returnType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input returnType(final SpellAbility sa, final String type, final Cost_Payment payment, final CostPart part, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nReturns = 0;

            @Override
            public void showMessage() {
                StringBuilder msg = new StringBuilder("Return ");
                int nLeft = nNeeded - nReturns;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }

                typeList = AllZoneUtil.getPlayerCardsInPlay(sa.getSourceCard().getController());
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (typeList.contains(card)) {
                    nReturns++;
                    AllZone.getGameAction().moveToHand(card);
                    typeList.remove(card);
                    //in case nothing else to return
                    if (nReturns == nNeeded)
                        done();
                    else if (typeList.size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }

            public void done() {
                stop();
                payment.paidCost(part);
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }
        };

        return target;
    }//returnType()  
}
