package forge.card.spellability;

import forge.*;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.mana.ManaCost;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCostUtil;

import javax.swing.*;

/**
 * <p>Cost_Payment class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Cost_Payment {
    private Cost cost = null;
    private SpellAbility ability = null;
    private Card card = null;
    private SpellAbility_Requirements req = null;

    /**
     * <p>Getter for the field <code>cost</code>.</p>
     *
     * @return a {@link forge.card.spellability.Cost} object.
     */
    public Cost getCost() {
        return cost;
    }

    /**
     * <p>Getter for the field <code>ability</code>.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getAbility() {
        return ability;
    }

    /**
     * <p>Getter for the field <code>card</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        return card;
    }

    /**
     * <p>setRequirements.</p>
     *
     * @param reqs a {@link forge.card.spellability.SpellAbility_Requirements} object.
     */
    public void setRequirements(SpellAbility_Requirements reqs) {
        req = reqs;
    }

    /**
     * <p>setCancel.</p>
     *
     * @param cancel a boolean.
     */
    public void setCancel(boolean cancel) {
        bCancel = cancel;
    }

    /**
     * <p>isCanceled.</p>
     *
     * @return a boolean.
     */
    public boolean isCanceled() {
        return bCancel;
    }

    // No default values so an error will be kicked if not set properly in constructor
    private boolean payTap;
    private boolean payUntap;
    private boolean payMana;
    private boolean payXMana;
    private boolean paySubCounter;
    private boolean payAddCounter;
    private boolean paySac;
    private boolean payExile;
    private boolean payExileFromHand;
    private boolean payExileFromGrave;
    private boolean payExileFromTop;
    private boolean payLife;
    private boolean payDiscard;
    private boolean payTapXType;
    private boolean payReturn;

    private boolean bCancel = false;
    private boolean bXDefined = true;

    private CardList payTapXTypeTappedList = new CardList();

    /**
     * <p>addPayTapXTypeTappedList.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    private void addPayTapXTypeTappedList(Card c) {
        payTapXTypeTappedList.add(c);
    }

    /**
     * <p>Setter for the field <code>payMana</code>.</p>
     *
     * @param bPay a boolean.
     */
    public void setPayMana(boolean bPay) {
        payMana = bPay;
    }

    /**
     * <p>Setter for the field <code>payXMana</code>.</p>
     *
     * @param bPay a boolean.
     */
    public void setPayXMana(boolean bPay) {
        payXMana = bPay;
    }

    /**
     * <p>Setter for the field <code>payDiscard</code>.</p>
     *
     * @param bSac a boolean.
     */
    public void setPayDiscard(boolean bSac) {
        payDiscard = bSac;
    }

    /**
     * <p>Setter for the field <code>paySac</code>.</p>
     *
     * @param bSac a boolean.
     */
    public void setPaySac(boolean bSac) {
        paySac = bSac;
    }

    /**
     * <p>Setter for the field <code>payExile</code>.</p>
     *
     * @param bExile a boolean.
     */
    public void setPayExile(boolean bExile) {
        payExile = bExile;
    }

    /**
     * <p>Setter for the field <code>payExileFromHand</code>.</p>
     *
     * @param bExileFromHand a boolean.
     */
    public void setPayExileFromHand(boolean bExileFromHand) {
        payExileFromHand = bExileFromHand;
    }

    /**
     * <p>Setter for the field <code>payExileFromGrave</code>.</p>
     *
     * @param bExileFromGrave a boolean.
     */
    public void setPayExileFromGrave(boolean bExileFromGrave) {
        payExileFromGrave = bExileFromGrave;
    }

    /**
     * <p>Setter for the field <code>payExileFromTop</code>.</p>
     *
     * @param bExileFromTop a boolean.
     */
    public void setPayExileFromTop(boolean bExileFromTop) {
        payExileFromTop = bExileFromTop;
    }

    /**
     * <p>Setter for the field <code>payTapXType</code>.</p>
     *
     * @param bTapX a boolean.
     */
    public void setPayTapXType(boolean bTapX) {
        payTapXType = bTapX;
    }

    /**
     * <p>Setter for the field <code>payReturn</code>.</p>
     *
     * @param bReturn a boolean.
     */
    public void setPayReturn(boolean bReturn) {
        payReturn = bReturn;
    }

    /**
     * <p>Constructor for Cost_Payment.</p>
     *
     * @param cost a {@link forge.card.spellability.Cost} object.
     * @param abil a {@link forge.card.spellability.SpellAbility} object.
     */
    public Cost_Payment(Cost cost, SpellAbility abil) {
        this.cost = cost;
        this.ability = abil;
        card = this.ability.getSourceCard();
        payTap = !cost.getTap();
        payUntap = !cost.getUntap();
        payMana = cost.hasNoManaCost();
        payXMana = cost.hasNoXManaCost();
        paySubCounter = !cost.getSubCounter();
        payAddCounter = !cost.getAddCounter();
        paySac = !cost.getSacCost();
        payExile = !cost.getExileCost();
        payExileFromHand = !cost.getExileFromHandCost();
        payExileFromGrave = !cost.getExileFromGraveCost();
        payExileFromTop = !cost.getExileFromTopCost();
        payLife = !cost.getLifeCost();
        payDiscard = !cost.getDiscardCost();
        payTapXType = !cost.getTapXTypeCost();
        payReturn = !cost.getReturnCost();
    }

    /**
     * <p>canPayAdditionalCosts.</p>
     *
     * @param cost a {@link forge.card.spellability.Cost} object.
     * @param ability a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(Cost cost, SpellAbility ability) {
        if (cost == null)
            return true;

        final Card card = ability.getSourceCard();
        if (cost.getTap() && (card.isTapped() || card.isSick()))
            return false;

        if (cost.getUntap() && (card.isUntapped() || card.isSick()))
            return false;

        if (cost.getTapXTypeCost()) {
            CardList typeList = AllZoneUtil.getPlayerCardsInPlay(card.getController());

            typeList = typeList.getValidCards(cost.getTapXType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());

            if (cost.getTap()) {
                typeList = typeList.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return !c.equals(card) && c.isUntapped();
                    }
                });
            }
            if (typeList.size() == 0)
                return false;
        }

        int countersLeft = 0;
        if (cost.getSubCounter()) {
            Counters c = cost.getCounterType();
            countersLeft = card.getCounters(c) - cost.getCounterNum();
            if (countersLeft < 0) {
                return false;
            }
        }

        if (cost.getAddCounter()) {
            // Adding Counters as a cost should always be able to be paid
        }

        if (cost.getLifeCost()) {
            if (!card.getController().canPayLife(cost.getLifeAmount())) return false;
        }

        if (cost.getDiscardCost()) {
            CardList handList = AllZoneUtil.getPlayerHand(card.getController());
            String discType = cost.getDiscardType();
            int discAmount = cost.getDiscardAmount();

            if (cost.getDiscardThis()) {
                if (!AllZone.getZone(card).getZoneName().equals(Constant.Zone.Hand))
                    return false;
            } else if (discType.equals("Hand")) {
                // this will always work
            } else if (discType.equals("LastDrawn")) {
                Card c = card.getController().getLastDrawnCard();
                CardList hand = AllZoneUtil.getPlayerHand(card.getController());
                return hand.contains(c);
            } else {
                if (!discType.equals("Any") && !discType.equals("Random")) {
                    String validType[] = discType.split(";");

                    handList = handList.getValidCards(validType, ability.getActivatingPlayer(), ability.getSourceCard());
                }
                if (discAmount > handList.size()) {
                    // not enough cards in hand to pay
                    return false;
                }
            }
        }

        if (cost.getSacCost()) {
            if (!cost.getSacThis()) {
                CardList typeList = AllZoneUtil.getPlayerCardsInPlay(card.getController());

                typeList = typeList.getValidCards(cost.getSacType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());

                int amount = cost.isSacAll() ? typeList.size() : cost.getSacAmount();

                if (typeList.size() < amount)
                    return false;
            } else if (!AllZoneUtil.isCardInPlay(card))
                return false;
        }

        if (cost.getExileCost()) {
            if (!cost.getExileThis()) {
                CardList typeList = AllZoneUtil.getPlayerCardsInPlay(card.getController());

                typeList = typeList.getValidCards(cost.getExileType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                if (typeList.size() < cost.getExileAmount())
                    return false;
            } else if (!AllZoneUtil.isCardInPlay(card))
                return false;
        }

        if (cost.getExileFromHandCost()) {
            if (!cost.getExileFromHandThis()) {
                CardList typeList = AllZoneUtil.getPlayerHand(card.getController());

                typeList = typeList.getValidCards(cost.getExileFromHandType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                if (typeList.size() < cost.getExileFromHandAmount())
                    return false;
            } else if (!AllZoneUtil.isCardInPlayerHand(card.getController(), card))
                return false;
        }

        if (cost.getExileFromGraveCost()) {
            if (!cost.getExileFromGraveThis()) {
                CardList typeList = AllZoneUtil.getPlayerGraveyard(card.getController());

                typeList = typeList.getValidCards(cost.getExileFromGraveType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                if (typeList.size() < cost.getExileFromGraveAmount())
                    return false;
            } else if (!AllZoneUtil.isCardInPlayerGraveyard(card.getController(), card))
                return false;
        }

        if (cost.getExileFromTopCost()) {
            if (!cost.getExileFromTopThis()) {
                CardList typeList = AllZoneUtil.getPlayerCardsInLibrary(card.getController());

                typeList = typeList.getValidCards(cost.getExileFromTopType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                if (typeList.size() < cost.getExileFromTopAmount())
                    return false;
            } else if (!AllZoneUtil.isCardInPlayerLibrary(card.getController(), card))
                return false;
        }

        if (cost.getReturnCost()) {
            if (!cost.getReturnThis()) {
                CardList typeList = AllZoneUtil.getPlayerCardsInPlay(card.getController());

                typeList = typeList.getValidCards(cost.getReturnType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                if (typeList.size() < cost.getReturnAmount())
                    return false;
            } else if (!AllZoneUtil.isCardInPlay(card))
                return false;
        }

        return true;
    }

    /**
     * <p>setInput.</p>
     *
     * @param in a {@link forge.gui.input.Input} object.
     */
    public void setInput(Input in) {
        AllZone.getInputControl().setInput(in, true);
    }

    /**
     * <p>payCost.</p>
     *
     * @return a boolean.
     */
    public boolean payCost() {
        if (bCancel) {
            req.finishPaying();
            return false;
        }

        if (!payTap && cost.getTap()) {
            if (card.isUntapped()) {
                card.tap();
                payTap = true;
            } else
                return false;
        }

        if (!payUntap && cost.getUntap()) {
            if (card.isTapped()) {
                card.untap();
                payUntap = true;
            } else
                return false;
        }

        int manaToAdd = 0;
        if (bXDefined && !cost.hasNoXManaCost()) {
            // if X cost is a defined value, other than xPaid
            if (!card.getSVar("X").equals("Count$xPaid")) {
                // this currently only works for things about Targeted object
                manaToAdd = AbilityFactory.calculateAmount(card, "X", ability) * cost.getXMana();
                payXMana = true;    // Since the X-cost is being lumped into the mana cost
                payMana = false;
            }
        }
        bXDefined = false;

        if (!payMana) {        // pay mana here
            setInput(input_payMana(getAbility(), this, manaToAdd));
            return false;
        }

        if (!payXMana && !cost.hasNoXManaCost()) {        // pay X mana here
            card.setXManaCostPaid(0);
            setInput(input_payXMana(getCost().getXMana(), getAbility(), this));
            return false;
        }

        if (!payTapXType && cost.getTapXTypeCost()) {
            CardList typeList = AllZoneUtil.getPlayerCardsInPlay(card.getController());
            typeList = typeList.getValidCards(cost.getTapXType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());

            setInput(input_tapXCost(cost.getTapXTypeAmount(), cost.getTapXType(), typeList, ability, this));
            return false;
        }

        if (!paySubCounter && cost.getSubCounter()) {    // pay counters here.
            Counters type = cost.getCounterType();
            if (card.getCounters(type) >= cost.getCounterNum()) {
                card.subtractCounter(type, cost.getCounterNum());
                paySubCounter = true;
            } else {
                bCancel = true;
                req.finishPaying();
                return false;
            }
        }

        if (!payAddCounter && cost.getAddCounter()) {    // add counters here.
            card.addCounterFromNonEffect(cost.getCounterType(), cost.getCounterNum());
            payAddCounter = true;
        }

        if (!payLife && cost.getLifeCost()) {            // pay life here
            StringBuilder sb = new StringBuilder();
            sb.append(getCard().getName());
            sb.append(" - Pay ");
            sb.append(cost.getLifeAmount());
            sb.append(" Life?");
            Object[] possibleValues = {"Yes", "No"};
            Object choice = JOptionPane.showOptionDialog(null, sb.toString(), getCard().getName() + " - Cost",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, possibleValues, possibleValues[0]);
            if (choice.equals(0)) {
                AllZone.getHumanPlayer().payLife(cost.getLifeAmount(), null);
                payLife = true;
            } else {
                bCancel = true;
                req.finishPaying();
                return false;
            }
        }

        if (!payDiscard && cost.getDiscardCost()) {            // discard here
            CardList handList = AllZoneUtil.getPlayerHand(card.getController());
            String discType = cost.getDiscardType();
            int discAmount = cost.getDiscardAmount();

            if (cost.getDiscardThis()) {
                card.getController().discard(card, ability);
                payDiscard = true;
            } else if (discType.equals("Hand")) {
                card.getController().discardHand(ability);
                payDiscard = true;
            } else if (discType.equals("LastDrawn")) {
                if (handList.contains(card.getController().getLastDrawnCard())) {
                    card.getController().discard(card.getController().getLastDrawnCard(), ability);
                    payDiscard = true;
                }

            } else {
                if (discType.equals("Random")) {
                    card.getController().discardRandom(discAmount, ability);
                    payDiscard = true;
                } else {
                    if (!discType.equals("Any")) {
                        String validType[] = discType.split(";");
                        handList = handList.getValidCards(validType, ability.getActivatingPlayer(), ability.getSourceCard());
                    }
                    setInput(input_discardCost(discAmount, discType, handList, ability, this));
                    return false;
                }
            }
        }

        if (!paySac && cost.getSacCost()) {                    // sacrifice stuff here
            if (cost.getSacThis())
                setInput(sacrificeThis(ability, this));
            else if (cost.isSacAll())
                sacrificeAllType(ability, cost.getSacType(), this);
            else if (cost.isSacX())
                setInput(sacrificeXType(ability, cost.getSacType(), this));
            else
                setInput(sacrificeType(ability, cost.getSacType(), this));
            return false;
        }

        if (!payExile && cost.getExileCost()) {                    // exile stuff here
            if (cost.getExileThis())
                setInput(exileThis(ability, this));
            else
                setInput(exileType(ability, cost.getExileType(), this));
            return false;
        }

        if (!payExileFromHand && cost.getExileFromHandCost()) {                    // exile stuff here
            if (cost.getExileFromHandThis())
                setInput(exileFromHandThis(ability, this));
            else
                setInput(exileFromHandType(ability, cost.getExileFromHandType(), this));
            return false;
        }

        if (!payExileFromGrave && cost.getExileFromGraveCost()) {                    // exile stuff here
            if (cost.getExileFromGraveThis())
                setInput(exileFromGraveThis(ability, this));
            else
                setInput(exileFromGraveType(ability, cost.getExileFromGraveType(), this));
            return false;
        }

        if (!payExileFromTop && cost.getExileFromTopCost()) {                    // exile stuff here
            if (cost.getExileFromTopThis())
                setInput(exileFromTopThis(ability, this));
            else
                setInput(exileFromTopType(ability, cost.getExileFromTopType(), this));
            return false;
        }

        if (!payReturn && cost.getReturnCost()) {                    // return stuff here
            if (cost.getReturnThis())
                setInput(returnThis(ability, this));
            else
                setInput(returnType(ability, cost.getReturnType(), this));
            return false;
        }

        resetUndoList();
        req.finishPaying();
        return true;
    }

    /**
     * <p>isAllPaid.</p>
     *
     * @return a boolean.
     */
    public boolean isAllPaid() {
        // if you add a new Cost type add it here
        return (payTap && payUntap && payMana && payXMana && paySubCounter && payAddCounter &&
                paySac && payExile && payLife && payDiscard && payTapXType && payReturn &&
                payExileFromHand && payExileFromGrave && payExileFromTop);
    }

    /**
     * <p>resetUndoList.</p>
     */
    public void resetUndoList() {
        // TODO: clear other undoLists here?
        payTapXTypeTappedList.clear();
    }

    /**
     * <p>cancelPayment.</p>
     */
    public void cancelPayment() {
        // unpay anything we can.
        if (cost.getTap() && payTap) {
            // untap if tapped
            card.untap();
        }
        if (cost.getUntap() && payUntap) {
            // tap if untapped
            card.tap();
        }
        // refund mana
        AllZone.getManaPool().unpaid(ability, false);

        if (cost.getTapXTypeCost()) { // Can't depend on payTapXType if canceling before tapping enough

            for (Card c : payTapXTypeTappedList)
                c.untap();
            //needed?
            payTapXTypeTappedList.clear();
        }

        // refund counters
        if (cost.getSubCounter() && paySubCounter) {
            card.addCounterFromNonEffect(cost.getCounterType(), cost.getCounterNum());
        }

        // remove added counters
        if (cost.getAddCounter() && payAddCounter) {
            card.subtractCounter(cost.getCounterType(), cost.getCounterNum());
        }

        // refund life
        if (cost.getLifeCost() && payLife) {
            card.getController().payLife(cost.getLifeAmount() * -1, null);
        }

        // can't really undiscard things

        // can't really unsacrifice things

        //can't really unexile things

        // can't really unexile things from hand

        // can't really unreturn things
    }

    /**
     * <p>payComputerCosts.</p>
     *
     * @return a boolean.
     */
    public boolean payComputerCosts() {
        // ******** NOTE for Adding Costs ************
        // make sure ComputerUtil.canPayAdditionalCosts() is updated so the AI knows if they can Pay the cost
        CardList sacCard = new CardList();
        CardList exileCard = new CardList();
        CardList exileFromHandCard = new CardList();
        CardList exileFromGraveCard = new CardList();
        CardList exileFromTopCard = new CardList();
        CardList tapXCard = new CardList();
        CardList returnCard = new CardList();
        ability.setActivatingPlayer(AllZone.getComputerPlayer());

        // double check if something can be sacrificed here. Real check is in ComputerUtil.canPayAdditionalCosts()
        if (cost.getSacCost()) {
            int amount = cost.getSacAmount();
            if (cost.getSacThis())
                sacCard.add(card);
            else if (cost.isSacAll()) {
                CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
                typeList = typeList.getValidCards(cost.getSacType().split(","), card.getController(), card);
                sacCard.addAll(typeList);
                amount = sacCard.size();
            } else
                sacCard = ComputerUtil.chooseSacrificeType(cost.getSacType(), card, ability.getTargetCard(), cost.getSacAmount());

            if (sacCard.size() != amount) {
                System.out.println("Couldn't find a valid card to sacrifice for: " + card.getName());
                return false;
            }
        }

        // double check if something can be exiled here. Real check is in ComputerUtil.canPayAdditionalCosts()
        if (cost.getExileCost()) {
            if (cost.getExileThis())
                exileCard.add(card);
            else
                exileCard = ComputerUtil.chooseExileType(cost.getExileType(), card, ability.getTargetCard(), cost.getExileAmount());


            if (exileCard.size() != cost.getExileAmount()) {
                System.out.println("Couldn't find a valid card to exile for: " + card.getName());
                return false;
            }
        }

        // double check if something can be exiled here. Real check is in ComputerUtil.canPayAdditionalCosts()
        if (cost.getExileFromHandCost()) {
            if (cost.getExileFromHandThis())
                exileFromHandCard.add(card);
            else
                exileFromHandCard = ComputerUtil.chooseExileFromHandType(cost.getExileFromHandType(), card, ability.getTargetCard(), cost.getExileFromHandAmount());

            if (exileFromHandCard.size() != cost.getExileFromHandAmount()) {
                System.out.println("Couldn't find a valid card to exile for: " + card.getName());
                return false;
            }
        }

        if (cost.getExileFromGraveCost()) {
            if (cost.getExileFromGraveThis())
                exileFromGraveCard.add(card);
            else
                exileFromGraveCard = ComputerUtil.chooseExileFromGraveType(
                        cost.getExileFromGraveType(), card, ability.getTargetCard(), cost.getExileFromGraveAmount());

            if (exileFromGraveCard.size() != cost.getExileFromGraveAmount()) {
                System.out.println("Couldn't find a valid card to exile for: " + card.getName());
                return false;
            }
        }

        if (cost.getExileFromTopCost()) {
            if (cost.getExileFromTopThis())
                exileFromTopCard.add(card);
            else
                exileFromTopCard = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getComputerPlayer(), cost.getExileFromTopAmount());

            if (exileFromTopCard.size() != cost.getExileFromTopAmount()) {
                System.out.println("Couldn't find a valid card to exile for: " + card.getName());
                return false;
            }
        }

        if (cost.getReturnCost()) {
            if (cost.getReturnThis())
                returnCard.add(card);
            else
                returnCard = ComputerUtil.chooseReturnType(cost.getReturnType(), card, ability.getTargetCard(), cost.getReturnAmount());

            if (returnCard.size() != cost.getReturnAmount()) {
                System.out.println("Couldn't find a valid card to return for: " + card.getName());
                return false;
            }
        }

        if (cost.getDiscardThis()) {
            if (!AllZoneUtil.getPlayerHand(card.getController()).contains(card.getController().getLastDrawnCard())) {
                return false;
            }
            if (!AllZone.getZone(card).getZoneName().equals(Constant.Zone.Hand))
                return false;
        }

        if (cost.getTapXTypeCost()) {
            boolean tap = cost.getTap();

            tapXCard = ComputerUtil.chooseTapType(cost.getTapXType(), card, tap, cost.getTapXTypeAmount());

            if (tapXCard == null || tapXCard.size() != cost.getTapXTypeAmount()) {
                System.out.println("Couldn't find a valid card to tap for: " + card.getName());
                return false;
            }
        }

        // double check if counters available? Real check is in ComputerUtil.canPayAdditionalCosts()
        if (cost.getSubCounter() && cost.getCounterNum() > card.getCounters(cost.getCounterType())) {
            System.out.println("Not enough " + cost.getCounterType() + " on " + card.getName());
            return false;
        }

        if (cost.getTap())
            card.tap();

        if (cost.getUntap())
            card.untap();

        if (!cost.hasNoManaCost())
            ComputerUtil.payManaCost(ability);

        if (cost.getTapXTypeCost()) {
            for (Card c : tapXCard)
                c.tap();
        }

        if (cost.getSubCounter())
            card.subtractCounter(cost.getCounterType(), cost.getCounterNum());

        if (cost.getAddCounter()) {
            card.addCounterFromNonEffect(cost.getCounterType(), cost.getCounterNum());
        }

        if (cost.getLifeCost())
            AllZone.getComputerPlayer().payLife(cost.getLifeAmount(), null);

        if (cost.getDiscardCost()) {
            String discType = cost.getDiscardType();
            int discAmount = cost.getDiscardAmount();

            if (cost.getDiscardThis()) {
                card.getController().discard(card, ability);
            } else if (discType.equals("Hand")) {
                card.getController().discardHand(ability);
            } else {
                if (discType.equals("Random")) {
                    card.getController().discardRandom(discAmount, ability);
                } else {
                    if (!discType.equals("Any")) {
                        String validType[] = discType.split(";");
                        AllZone.getGameAction().AI_discardNumType(discAmount, validType, ability);
                    } else {
                        AllZone.getComputerPlayer().discard(discAmount, ability, false);
                    }
                }
            }
        }

        if (cost.getSacCost()) {
            for (Card c : sacCard)
                AllZone.getGameAction().sacrifice(c);
        }

        if (cost.getExileCost()) {
            for (Card c : exileCard)
                AllZone.getGameAction().exile(c);
        }

        if (cost.getExileFromHandCost()) {
            for (Card c : exileFromHandCard)
                AllZone.getGameAction().exile(c);
        }

        if (cost.getExileFromGraveCost()) {
            for (Card c : exileFromGraveCard)
                AllZone.getGameAction().exile(c);
        }

        if (cost.getExileFromTopCost()) {
            for (Card c : exileFromTopCard)
                AllZone.getGameAction().exile(c);
        }

        if (cost.getReturnCost()) {
            for (Card c : returnCard)
                AllZone.getGameAction().moveToHand(c);
        }
        return true;
    }

    /**
     * <p>changeCost.</p>
     */
    public void changeCost() {
        cost.changeCost(ability);
    }


    // ******************************************************************************
    // *********** Inputs used by Cost_Payment below here ***************************
    // ******************************************************************************

    /**
     * <p>input_payMana.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @param manaToAdd a int.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_payMana(final SpellAbility sa, final Cost_Payment payment, int manaToAdd) {
        final ManaCost manaCost;

        if (Phase.getGameBegins() == 1) {
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {
                String mana = payment.getCost().getMana().replace("X", "").trim();
                manaCost = new ManaCost(mana);
                manaCost.increaseColorlessMana(manaToAdd);
            }
        } else {
            manaCost = new ManaCost(sa.getManaCost());
        }

        Input payMana = new Input() {
            private ManaCost mana = manaCost;
            private static final long serialVersionUID = 3467312982164195091L;

            private final String originalManaCost = payment.getCost().getMana();

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
                if (phyLifeToLose > 0)
                    AllZone.getHumanPlayer().payLife(phyLifeToLose, sa.getSourceCard());
                sa.getSourceCard().setColorsPaid(mana.getColorsPaid());
                sa.getSourceCard().setSunburstValue(mana.getSunburst());
                resetManaCost();
                payment.setPayMana(true);
                stop();
                payment.payCost();
            }

            @Override
            public void selectButtonCancel() {
                resetManaCost();
                payment.setCancel(true);
                payment.payCost();
                AllZone.getHumanBattlefield().updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap
                stop();
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
     *
     * @param numX a int.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_payXMana(final int numX, final SpellAbility sa, final Cost_Payment payment) {
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
                payment.setCancel(true);
                payment.payCost();
                AllZone.getHumanBattlefield().updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap
                stop();
            }

            @Override
            public void selectButtonOK() {
                payment.setPayXMana(true);
                payment.getCard().setXManaCostPaid(xPaid);
                stop();
                payment.payCost();
            }

        };

        return payX;
    }


    /**
     * <p>input_discardCost.</p>
     *
     * @param nCards a int.
     * @param discType a {@link java.lang.String} object.
     * @param handList a {@link forge.CardList} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_discardCost(final int nCards, final String discType, final CardList handList, SpellAbility sa, final Cost_Payment payment) {
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
                if (nCards > 1) {
                    sb.append(" You have ");
                    sb.append(nCards - nDiscard);
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
                    if (nDiscard == nCards)
                        done();
                    else if (AllZone.getHumanHand().size() == 0)    // this really shouldn't happen
                        cancel();
                    else
                        showMessage();
                }
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }

            public void done() {
                payment.setPayDiscard(true);
                stop();
                payment.payCost();
            }
        };

        return target;
    }//input_discard() 

    /**
     * <p>sacrificeThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeThis(final SpellAbility sa, final Cost_Payment payment) {
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
                        payment.setPaySac(true);
                        payment.getAbility().addCostToHashList(card, "Sacrificed");
                        AllZone.getGameAction().sacrifice(card);
                        stop();
                        payment.payCost();
                    } else {
                        payment.setCancel(true);
                        stop();
                        payment.payCost();
                    }
                }
            }
        };

        return target;
    }//input_sacrifice()

    /**
     * <p>sacrificeType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeType(final SpellAbility sa, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nSacrifices = 0;
            private int nNeeded = payment.getCost().getSacAmount();

            @Override
            public void showMessage() {
                StringBuilder msg = new StringBuilder("Sacrifice ");
                int nLeft = nNeeded - nSacrifices;
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
                payment.setPaySac(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };

        return target;
    }//sacrificeType()

    /**
     * <p>sacrificeAllType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     */
    public static void sacrificeAllType(final SpellAbility sa, final String type, final Cost_Payment payment) {
        // TODO Ask First

        CardList typeList;
        typeList = AllZoneUtil.getPlayerCardsInPlay(sa.getActivatingPlayer());
        typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());

        for (Card card : typeList) {
            payment.getAbility().addCostToHashList(card, "Sacrificed");
            AllZone.getGameAction().sacrifice(card);
        }

        payment.setPaySac(true);
        payment.payCost();
    }

    /**
     * <p>sacrificeXType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeXType(final SpellAbility sa, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = -4496270321029213839L;
            private CardList typeList;
            private int nSacrifices = 0;

            @Override
            public void showMessage() {
                StringBuilder msg = new StringBuilder("Sacrifice X ");
                msg.append(type).append("s. ");
                msg.append("(").append(nSacrifices).append(" sacrificed so far.)");

                typeList = AllZoneUtil.getPlayerCardsInPlay(sa.getSourceCard().getController());
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableAll();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectButtonOK() {
                done();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if (typeList.contains(card)) {
                    nSacrifices++;
                    payment.getAbility().addCostToHashList(card, "Sacrificed");
                    AllZone.getGameAction().sacrifice(card);
                    typeList.remove(card);
                    if (typeList.size() == 0)    // this really shouldn't happen
                        done();
                    else
                        showMessage();
                }
            }

            public void done() {
                payment.setPaySac(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };

        return target;
    }//sacrificeXType()

    /**
     * <p>exileThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileThis(final SpellAbility sa, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 678668673002725001L;

            @Override
            public void showMessage() {
                Card card = sa.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlay(card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Exile?");
                    Object[] possibleValues = {"Yes", "No"};
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, possibleValues, possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.setPayExile(true);
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        AllZone.getGameAction().exile(card);
                        stop();
                        payment.payCost();
                    } else {
                        payment.setCancel(true);
                        stop();
                        payment.payCost();
                    }
                }
            }
        };

        return target;
    }//input_exile()

    /**
     * <p>exileFromHandThis.</p>
     *
     * @param spell a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileFromHandThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2651542083913697972L;

            @Override
            public void showMessage() {
                Card card = spell.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlayerHand(card.getController(), card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Exile?");
                    Object[] possibleValues = {"Yes", "No"};
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, possibleValues, possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.setPayExileFromHand(true);
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        AllZone.getGameAction().exile(card);
                        stop();
                        payment.payCost();
                    } else {
                        payment.setCancel(true);
                        stop();
                        payment.payCost();
                    }
                }
            }
        };
        return target;
    }//input_exile()

    /**
     * <p>exileFromTopThis.</p>
     *
     * @param spell a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileFromTopThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 3416809678763443014L;

            @Override
            public void showMessage() {
                Card card = spell.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlayerHand(card.getController(), card)) {
                    //This can't really happen, but if for some reason it could....
                    if (AllZoneUtil.getPlayerCardsInLibrary(card.getController()).size() > 0) {
                        payment.setPayExileFromTop(true);
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        AllZone.getGameAction().exile(card);
                        stop();
                        payment.payCost();
                    } else {
                        payment.setCancel(true);
                        stop();
                        payment.payCost();
                    }
                }
            }
        };
        return target;
    }//input_exile()

    /**
     * <p>exileFromGraveThis.</p>
     *
     * @param spell a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileFromGraveThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 6237561876518762902L;

            @Override
            public void showMessage() {
                Card card = spell.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlayerGraveyard(card.getController(), card)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Exile?");
                    Object[] possibleValues = {"Yes", "No"};
                    Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, possibleValues, possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.setPayExileFromGrave(true);
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        AllZone.getGameAction().exile(card);
                        stop();
                        payment.payCost();
                    } else {
                        payment.setCancel(true);
                        stop();
                        payment.payCost();
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
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileType(final SpellAbility sa, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 1403915758082824694L;

            private CardList typeList;
            private int nExiles = 0;
            private int nNeeded = payment.getCost().getExileAmount();

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
                payment.setPayExile(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };

        return target;
    }//exileType()

    /**
     * <p>exileFromHandType.</p>
     *
     * @param spell a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileFromHandType(final SpellAbility spell, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 759041801001973859L;
            private CardList typeList;
            private int nExiles = 0;
            private int nNeeded = payment.getCost().getExileFromHandAmount();

            @Override
            public void showMessage() {
                StringBuilder msg = new StringBuilder("Exile ");
                int nLeft = nNeeded - nExiles;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }
                msg.append(" from your hand");

                typeList = AllZoneUtil.getPlayerHand(spell.getSourceCard().getController());
                typeList = typeList.getValidCards(type.split(";"), spell.getActivatingPlayer(), spell.getSourceCard());
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
                payment.setPayExileFromHand(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };
        return target;
    }//exileFromHandType()

    /**
     * <p>exileFromGraveType.</p>
     *
     * @param spell a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileFromGraveType(final SpellAbility spell, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 734256837615635021L;

            @Override
            public void showMessage() {
                CardList typeList;
                int nNeeded = payment.getCost().getExileFromGraveAmount();
                typeList = AllZoneUtil.getPlayerGraveyard(spell.getSourceCard().getController());
                typeList = typeList.getValidCards(type.split(";"), spell.getActivatingPlayer(), spell.getSourceCard());

                for (int i = 0; i < nNeeded; i++) {
                    if (typeList.size() == 0)
                        cancel();

                    Object o = GuiUtils.getChoiceOptional("Exile from grave", typeList.toArray());

                    if (o != null) {
                        Card c = (Card) o;
                        typeList.remove(c);
                        payment.getAbility().addCostToHashList(c, "Exiled");
                        AllZone.getGameAction().exile(c);
                        if (i == nNeeded - 1) done();
                    }
                }
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            public void done() {
                payment.setPayExileFromGrave(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };
        return target;
    }//exileFromGraveType()

    /**
     * <p>exileFromTopType.</p>
     *
     * @param spell a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input exileFromTopType(final SpellAbility spell, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = -4764871768555887091L;

            @Override
            public void showMessage() {
                CardList typeList;
                int nNeeded = payment.getCost().getExileFromTopAmount();
                PlayerZone lib = AllZone.getZone(Constant.Zone.Library, spell.getSourceCard().getController());
                typeList = AllZoneUtil.getPlayerCardsInLibrary(spell.getSourceCard().getController());
                typeList = typeList.getValidCards(type.split(";"), spell.getActivatingPlayer(), spell.getSourceCard());

                for (int i = 0; i < nNeeded; i++) {
                    if (typeList.size() == 0)
                        cancel();

                    if (lib.size() > 0) {
                        Card c = typeList.get(0);
                        typeList.remove(c);
                        payment.getAbility().addCostToHashList(c, "Exiled");
                        AllZone.getGameAction().exile(c);
                        if (i == nNeeded - 1) done();
                    }
                }
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            public void done() {
                payment.setPayExileFromTop(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };
        return target;
    }//exileFromTopType()

    /**
     * <p>input_tapXCost.</p>
     *
     * @param nCards a int.
     * @param cardType a {@link java.lang.String} object.
     * @param cardList a {@link forge.CardList} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_tapXCost(final int nCards, final String cardType, final CardList cardList, SpellAbility sa, final Cost_Payment payment) {
        //final SpellAbility sp = sa;
        Input target = new Input() {

            private static final long serialVersionUID = 6438988130447851042L;
            int nTapped = 0;

            @Override
            public void showMessage() {
                if (cardList.size() == 0) stop();

                int left = nCards - nTapped;
                AllZone.getDisplay().showMessage("Select a " + cardType + " to tap (" + left + " left)");
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
                    payment.addPayTapXTypeTappedList(card);
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
                payment.setCancel(true);
                stop();
                payment.payCost();
            }

            public void done() {
                payment.setPayTapXType(true);
                stop();
                payment.payCost();
            }
        };

        return target;
    }//input_tapXCost() 

    /**
     * <p>returnThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input returnThis(final SpellAbility sa, final Cost_Payment payment) {
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
                        payment.setPayReturn(true);
                        AllZone.getGameAction().moveToHand(card);
                        stop();
                        payment.payCost();
                    } else {
                        payment.setCancel(true);
                        stop();
                        payment.payCost();
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
     * @param payment a {@link forge.card.spellability.Cost_Payment} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input returnType(final SpellAbility sa, final String type, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nReturns = 0;
            private int nNeeded = payment.getCost().getReturnAmount();

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
                payment.setPayReturn(true);
                stop();
                payment.payCost();
            }

            public void cancel() {
                payment.setCancel(true);
                stop();
                payment.payCost();
            }
        };

        return target;
    }//returnType()  
}
