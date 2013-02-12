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
package forge.card.cost;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostExile.
 */
public class CostExile extends CostPartWithList {
    // Exile<Num/Type{/TypeDescription}>
    // ExileFromHand<Num/Type{/TypeDescription}>
    // ExileFromGrave<Num/Type{/TypeDescription}>
    // ExileFromTop<Num/Type{/TypeDescription}> (of library)
    // ExileSameGrave<Num/Type{/TypeDescription}>

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileFrom extends Input {
        private final SpellAbility sa;
        private final String type;
        private final int nNeeded;
        private final CostPayment payment;
        private final CostExile part;
        private static final long serialVersionUID = 734256837615635021L;
        private List<Card> typeList;

        /**
         * TODO: Write javadoc for Constructor.
         * @param sa
         * @param type
         * @param nNeeded
         * @param payment
         * @param part
         */
        private InputExileFrom(SpellAbility sa, String type, int nNeeded, CostPayment payment, CostExile part) {
            this.sa = sa;
            this.type = type;
            this.nNeeded = nNeeded;
            this.payment = payment;
            this.part = part;
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            this.typeList = new ArrayList<Card>(sa.getActivatingPlayer().getCardsIn(part.getFrom()));
            this.typeList = CardLists.getValidCards(this.typeList, type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());

            for (int i = 0; i < nNeeded; i++) {
                if (this.typeList.size() == 0) {
                    this.cancel();
                }

                final Card c = GuiChoose.oneOrNone("Exile from " + part.getFrom(), this.typeList);

                if (c != null) {
                    this.typeList.remove(c);
                    part.addToList(c);
                    Singletons.getModel().getGame().getAction().exile(c);
                    if (i == (nNeeded - 1)) {
                        this.done();
                    }
                } else {
                    this.cancel();
                    break;
                }
            }
        }

        @Override
        public void selectButtonCancel() {
            this.cancel();
        }

        public void done() {
            this.stop();
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        }

        public void cancel() {
            this.stop();
            payment.cancelCost();
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileFromSame extends Input {
        private final List<Card> list;
        private final CostExile part;
        private final CostPayment payment;
        private final SpellAbility sa;
        private final int nNeeded;
        private final List<Player> payableZone;
        private static final long serialVersionUID = 734256837615635021L;
        private List<Card> typeList;

        /**
         * TODO: Write javadoc for Constructor.
         * @param list
         * @param part
         * @param payment
         * @param sa
         * @param nNeeded
         * @param payableZone
         */
        private InputExileFromSame(List<Card> list, CostExile part, CostPayment payment, SpellAbility sa, int nNeeded,
                List<Player> payableZone) {
            this.list = list;
            this.part = part;
            this.payment = payment;
            this.sa = sa;
            this.nNeeded = nNeeded;
            this.payableZone = payableZone;
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Exile from whose ");
            sb.append(part.getFrom().toString());
            sb.append("?");

            final Player p = GuiChoose.oneOrNone(sb.toString(), payableZone);
            if (p == null) {
                this.cancel();
            }

            typeList = CardLists.filter(list, CardPredicates.isOwner(p));

            for (int i = 0; i < nNeeded; i++) {
                if (this.typeList.size() == 0) {
                    this.cancel();
                }

                final Card c = GuiChoose.oneOrNone("Exile from " + part.getFrom(), this.typeList);

                if (c != null) {
                    this.typeList.remove(c);
                    part.addToList(c);
                    Singletons.getModel().getGame().getAction().exile(c);
                    if (i == (nNeeded - 1)) {
                        this.done();
                    }
                } else {
                    this.cancel();
                    break;
                }
            }
        }

        @Override
        public void selectButtonCancel() {
            this.cancel();
        }

        public void done() {
            this.stop();
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        }

        public void cancel() {
            this.stop();
            payment.cancelCost();
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileFromStack extends Input {
        private final CostPayment payment;
        private final SpellAbility sa;
        private final String type;
        private final int nNeeded;
        private final CostExile part;
        private static final long serialVersionUID = 734256837615635021L;
        private ArrayList<SpellAbility> saList;
        private ArrayList<String> descList;

        /**
         * TODO: Write javadoc for Constructor.
         * @param payment
         * @param sa
         * @param type
         * @param nNeeded
         * @param part
         */
        private InputExileFromStack(CostPayment payment, SpellAbility sa, String type, int nNeeded, CostExile part) {
            this.payment = payment;
            this.sa = sa;
            this.type = type;
            this.nNeeded = nNeeded;
            this.part = part;
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            saList = new ArrayList<SpellAbility>();
            descList = new ArrayList<String>();

            for (int i = 0; i < Singletons.getModel().getGame().getStack().size(); i++) {
                final Card stC = Singletons.getModel().getGame().getStack().peekAbility(i).getSourceCard();
                final SpellAbility stSA = Singletons.getModel().getGame().getStack().peekAbility(i).getRootAbility();
                if (stC.isValid(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard()) && stSA.isSpell()) {
                    this.saList.add(stSA);
                    if (stC.isCopiedSpell()) {
                        this.descList.add(stSA.getStackDescription() + " (Copied Spell)");
                    } else {
                        this.descList.add(stSA.getStackDescription());
                    }
                }
            }

            for (int i = 0; i < nNeeded; i++) {
                if (this.saList.isEmpty()) {
                    this.cancel();
                }

                //Have to use the stack descriptions here because some copied spells have no description otherwise
                final String o = GuiChoose.oneOrNone("Exile from " + part.getFrom(), this.descList);

                if (o != null) {
                    final SpellAbility toExile = this.saList.get(descList.indexOf(o));
                    final Card c = toExile.getSourceCard();
                    this.saList.remove(toExile);
                    part.addToList(c);
                    if (!c.isCopiedSpell()) {
                        Singletons.getModel().getGame().getAction().exile(c);
                    }
                    if (i == (nNeeded - 1)) {
                        this.done();
                    }
                    final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(toExile);
                    Singletons.getModel().getGame().getStack().remove(si);
                } else {
                    this.cancel();
                    break;
                }
            }
        }

        @Override
        public void selectButtonCancel() {
            this.cancel();
        }

        public void done() {
            this.stop();
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        }

        public void cancel() {
            this.stop();
            payment.cancelCost();
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileType extends Input {
        private final CostExile part;
        private final CostPayment payment;
        private final String type;
        private final int nNeeded;
        private final SpellAbility sa;
        private static final long serialVersionUID = 1403915758082824694L;
        private List<Card> typeList;
        private int nExiles = 0;

        /**
         * TODO: Write javadoc for Constructor.
         * @param part
         * @param payment
         * @param type
         * @param nNeeded
         * @param sa
         */
        private InputExileType(CostExile part, CostPayment payment, String type, int nNeeded, SpellAbility sa) {
            this.part = part;
            this.payment = payment;
            this.type = type;
            this.nNeeded = nNeeded;
            this.sa = sa;
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            final StringBuilder msg = new StringBuilder("Exile ");
            final int nLeft = nNeeded - this.nExiles;
            msg.append(nLeft).append(" ");
            msg.append(type);
            if (nLeft > 1) {
                msg.append("s");
            }

            if (part.getFrom().equals(ZoneType.Hand)) {
                msg.append(" from your Hand");
            } else if (part.getFrom().equals(ZoneType.Stack)) {
                msg.append(" from the Stack");
            }
            this.typeList = new ArrayList<Card>(sa.getActivatingPlayer().getCardsIn(part.getFrom()));
            this.typeList = CardLists.getValidCards(this.typeList, type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
            CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
            ButtonUtil.enableOnlyCancel();
        }

        @Override
        public void selectButtonCancel() {
            this.cancel();
        }

        @Override
        public void selectCard(final Card card) {
            if (this.typeList.contains(card)) {
                this.nExiles++;
                part.addToList(card);
                Singletons.getModel().getGame().getAction().exile(card);
                this.typeList.remove(card);
                // in case nothing else to exile
                if (this.nExiles == nNeeded) {
                    this.done();
                } else if (this.typeList.size() == 0) {
                    // happen
                    this.cancel();
                } else {
                    this.showMessage();
                }
            }
        }

        public void done() {
            this.stop();
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        }

        public void cancel() {
            this.stop();
            payment.cancelCost();
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileThis extends Input {
        private final CostPayment payment;
        private final CostExile part;
        private final SpellAbility sa;
        private static final long serialVersionUID = 678668673002725001L;

        /**
         * TODO: Write javadoc for Constructor.
         * @param payment
         * @param part
         * @param sa
         */
        private InputExileThis(CostPayment payment, CostExile part, SpellAbility sa) {
            this.payment = payment;
            this.part = part;
            this.sa = sa;
        }

        @Override
        public void showMessage() {
            final Card card = sa.getSourceCard();
            if ( sa.getActivatingPlayer().getZone(part.getFrom()).contains(card)) {

                boolean choice = GuiDialog.confirm(card, card.getName() + " - Exile?");
                if (choice) {
                    payment.getAbility().addCostToHashList(card, "Exiled");
                    Singletons.getModel().getGame().getAction().exile(card);
                    part.addToList(card);
                    this.stop();
                    part.addListToHash(sa, "Exiled");
                    payment.paidCost(part);
                } else {
                    this.stop();
                    payment.cancelCost();
                }
            }
        }
    }

    private ZoneType from = ZoneType.Battlefield;
    private boolean sameZone = false;

    /**
     * Gets the from.
     * 
     * @return the from
     */
    public final ZoneType getFrom() {
        return this.from;
    }

    /**
     * Instantiates a new cost exile.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     * @param from
     *            the from
     */
    public CostExile(final String amount, final String type, final String description, final ZoneType from) {
        super(amount, type, description);
        if (from != null) {
            this.from = from;
        }
    }

    public CostExile(final String amount, final String type, final String description, final ZoneType from, final boolean sameZone) {
        this(amount, type, description, from);
        this.sameZone = sameZone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Exile ");

        if (this.isTargetingThis()) {
            sb.append(this.getType());
            if (!this.from.equals(ZoneType.Battlefield)) {
                sb.append(" from your ").append(this.from);
            }
            return sb.toString();
        } else if (this.getType().equals("All")) {
            sb.append(" all cards from your ").append(this.from);
            return sb.toString();
        }

        if (this.from.equals(ZoneType.Battlefield)) {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));
            if (!this.isTargetingThis()) {
                sb.append(" you control");
            }
            return sb.toString();
        }

        if (i != null) {
            sb.append(i);
        } else {
            sb.append(this.getAmount());
        }
        if (!this.getType().equals("Card")) {
            sb.append(" " + this.getType());
        }
        sb.append(" card");
        if ((i == null) || (i > 1)) {
            sb.append("s");
        }

        if (this.sameZone) {
            sb.append(" from the same ");
        } else {
            sb.append(" from your ");
        }

        sb.append(this.from);

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost, final GameState game) {
        List<Card> typeList = new ArrayList<Card>();
        if (this.getType().equals("All")) {
            return true; // this will always work
        }
        if (this.getFrom().equals(ZoneType.Stack)) {
            for (int i = 0; i < Singletons.getModel().getGame().getStack().size(); i++) {
                typeList.add(Singletons.getModel().getGame().getStack().peekAbility(i).getSourceCard());
            }
        } else {
            if (this.sameZone) {
                typeList = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
            } else {
                typeList = new ArrayList<Card>(activator.getCardsIn(this.getFrom()));
            }
        }
        if (!this.isTargetingThis()) {
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

            final Integer amount = this.convertAmount();
            if ((amount != null) && (typeList.size() < amount)) {
                return false;
            }

            if (this.sameZone && amount != null) {
                boolean foundPayable = false;
                List<Player> players = game.getPlayers();
                for (Player p : players) {
                    if (CardLists.filter(typeList, CardPredicates.isController(p)).size() >= amount) {
                        foundPayable = true;
                        break;
                    }
                }
                if (!foundPayable) {
                    return false;
                }
            }
        } else if (!typeList.contains(source)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        for (final Card c : this.getList()) {
            Singletons.getModel().getGame().getAction().exile(c);
            if (this.from.equals(ZoneType.Stack)) {
                ArrayList<SpellAbility> spells = c.getSpellAbilities();
                for (SpellAbility spell : spells) {
                    if (c.isInZone(ZoneType.Exile)) {
                        final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(spell);
                        Singletons.getModel().getGame().getStack().remove(si);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Player activator = ability.getActivatingPlayer();
        List<Card> list;

        if (this.sameZone) {
            list = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
        } else {
            list = new ArrayList<Card>(activator.getCardsIn(this.getFrom()));
        }

        if (this.getType().equals("All")) {
            this.setList(list);
            for (final Card card : list) {
                Singletons.getModel().getGame().getAction().exile(card);
            }
            payment.paidCost(this);
        }
        list = CardLists.getValidCards(list, this.getType().split(";"), activator, source);
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, list.size());
            } else if (sVar.equals("YChoice")) {
                c = CostUtil.chooseYValue(source, ability, list.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        
        Input target = null;
        if (this.isTargetingThis()) {
            target = new InputExileThis(payment, this, ability);
        } else if (this.from.equals(ZoneType.Battlefield) || this.from.equals(ZoneType.Hand)) {
            target = new InputExileType(this, payment, this.getType(), c, ability);
        } else if (this.from.equals(ZoneType.Stack)) {
            target = new InputExileFromStack(payment, ability, this.getType(), c, this);
        } else if (this.from.equals(ZoneType.Library)) {
            // this does not create input
            CostExile.exileFromTop(ability, this, payment, c);
        } else if (this.sameZone) {
            List<Player> players = game.getPlayers();
            List<Player> payableZone = new ArrayList<Player>();
            for (Player p : players) {
                List<Card> enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
                if (enoughType.size() < c) {
                    list.removeAll(enoughType);
                } else {
                    payableZone.add(p);
                }
            }
            target = new InputExileFromSame(list, this, payment, ability, c, payableZone);
        } else {
            target = new InputExileFrom(ability, this.getType(), c, payment, this);
        }
        if ( null != target )
            Singletons.getModel().getMatch().getInput().setInputInterrupt(target);
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        this.resetList();
        if (this.isTargetingThis()) {
            this.getList().add(source);
        } else if (this.getType().equals("All")) {
            this.setList(new ArrayList<Card>(ability.getActivatingPlayer().getCardsIn(this.getFrom())));
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                final String sVar = ability.getSVar(this.getAmount());
                // Generalize this
                if (sVar.equals("XChoice")) {
                    return false;
                }

                if (sVar.equals("YChoice")) {
                        return false;
                }

                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }

            if (this.from.equals(ZoneType.Library)) {
                this.setList(ai.getCardsIn(ZoneType.Library, c));
            } else if (this.sameZone) {
              // TODO Determine exile from same zone for AI
              return false;
            } else {
                this.setList(ComputerUtil.chooseExileFrom(ai, this.getFrom(), this.getType(), source,
                        ability.getTargetCard(), c));
            }
            if ((this.getList() == null) || (this.getList().size() < c)) {
                return false;
            }
        }
        return true;
    }

    // Inputs

    /**
     * Exile from top.
     * 
     * @param sa
     *            the sa
     * @param part
     *            the part
     * @param payment
     *            the payment
     * @param nNeeded
     *            the n needed
     */
    public static void exileFromTop(final SpellAbility sa, final CostExile part, final CostPayment payment,
            final int nNeeded) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Exile ").append(nNeeded).append(" cards from the top of your library?");
        final List<Card> list = sa.getActivatingPlayer().getCardsIn(ZoneType.Library, nNeeded);

        if (list.size() > nNeeded) {
            // I don't believe this is possible
            payment.cancelCost();
            return;
        }

        final boolean doExile = GuiDialog.confirm(sa.getSourceCard(), sb.toString());
        if (doExile) {
            final Iterator<Card> itr = list.iterator();
            while (itr.hasNext()) {
                final Card c = itr.next();
                part.addToList(c);
                Singletons.getModel().getGame().getAction().exile(c);
            }
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        } else {
            payment.cancelCost();
        }
    }
}
