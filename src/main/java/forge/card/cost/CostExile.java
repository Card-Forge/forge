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
import forge.FThreads;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.control.input.InputPayment;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.MagicStack;
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

    private static final class InputExileFrom extends InputPayCostBase {
        private final SpellAbility sa;
        private final String type;
        private final int nNeeded;
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
        private InputExileFrom(SpellAbility sa, String type, int nNeeded, CostExile part) {
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

            this.typeList = new ArrayList<Card>(sa.getActivatingPlayer().getCardsIn(part.getFrom()));
            this.typeList = CardLists.getValidCards(this.typeList, type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());

            for (int i = 0; i < nNeeded; i++) {
                if (this.typeList.size() == 0) {
                    this.onCancel();
                }

                final Card c = GuiChoose.oneOrNone("Exile from " + part.getFrom(), this.typeList);

                if (c != null) {
                    this.typeList.remove(c);
                    part.executePayment(sa, c);
                    if (i == (nNeeded - 1)) {
                        this.done();
                    }
                } else {
                    this.onCancel();
                    break;
                }
            }
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileFromSame extends InputPayCostBase {
        private final List<Card> list;
        private final CostExile part;
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
        private InputExileFromSame(List<Card> list, CostExile part, int nNeeded, List<Player> payableZone) {
            this.list = list;
            this.part = part;
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
                this.onCancel();
            }

            typeList = CardLists.filter(list, CardPredicates.isOwner(p));

            for (int i = 0; i < nNeeded; i++) {
                if (this.typeList.size() == 0) {
                    this.onCancel();
                }

                final Card c = GuiChoose.oneOrNone("Exile from " + part.getFrom(), this.typeList);

                if (c != null) {
                    this.typeList.remove(c);
                    part.executePayment(null, c);
                    if (i == (nNeeded - 1)) {
                        this.done();
                    }
                } else {
                    this.onCancel();
                    break;
                }
            }
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileFromStack extends InputPayCostBase {

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
        private InputExileFromStack(SpellAbility sa, String type, int nNeeded, CostExile part) {
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
            final MagicStack stack = sa.getActivatingPlayer().getGame().getStack();

            for (int i = 0; i < stack.size(); i++) {
                final Card stC = stack.peekAbility(i).getSourceCard();
                final SpellAbility stSA = stack.peekAbility(i).getRootAbility();
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
                    this.onCancel();
                }

                //Have to use the stack descriptions here because some copied spells have no description otherwise
                final String o = GuiChoose.oneOrNone("Exile from " + part.getFrom(), this.descList);

                if (o != null) {
                    final SpellAbility toExile = this.saList.get(descList.indexOf(o));
                    final Card c = toExile.getSourceCard();
                    

                    this.saList.remove(toExile);
                    if (!c.isCopiedSpell()) {
                        part.executePayment(sa, c);
                    } else
                        part.addToList(c);

                    if (i == (nNeeded - 1)) {
                        this.done();
                    }
                    final SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(toExile);
                    stack.remove(si);
                } else {
                    this.onCancel();
                    break;
                }
            }
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileType extends InputPayCostBase {
        private final CostExile part;
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
        private InputExileType(CostExile part, String type, int nNeeded, SpellAbility sa) {
            this.part = part;
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
        protected void onCardSelected(Card card) {
            if (this.typeList.contains(card)) {
                this.nExiles++;
                part.executePayment(sa, card);
                this.typeList.remove(card);
                // in case nothing else to exile
                if (this.nExiles == nNeeded) {
                    this.done();
                } else if (this.typeList.size() == 0) {
                    // happen
                    this.onCancel();
                } else {
                    this.showMessage();
                }
            }
        }
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private static final class InputExileThis extends InputPayCostBase {
        private final CostExile part;
        private final SpellAbility sa;
        private static final long serialVersionUID = 678668673002725001L;

        /**
         * TODO: Write javadoc for Constructor.
         * @param payment
         * @param part
         * @param sa
         */
        private InputExileThis(CostExile part, SpellAbility sa) {
            this.part = part;
            this.sa = sa;
        }
        @Override
        public void showMessage() {
            final Card card = sa.getSourceCard();
            if ( sa.getActivatingPlayer().getZone(part.getFrom()).contains(card)) {
                boolean choice = GuiDialog.confirm(card, card.getName() + " - Exile?");
                if (choice) {
                    part.executePayment(sa, card);
                    done();
                    return;
                }
            }
            onCancel();
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

        if (this.payCostFromSource()) {
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
            if (!this.payCostFromSource()) {
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
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getSourceCard();
        final GameState game = activator.getGame();
        
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
        if (!this.payCostFromSource()) {
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
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final GameState game) {
        final String amount = this.getAmount();
        final Card source = ability.getSourceCard();
        Integer c = this.convertAmount();
        final Player activator = ability.getActivatingPlayer();
        List<Card> list;

        if (this.sameZone) {
            list = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
        } else {
            list = new ArrayList<Card>(activator.getCardsIn(this.getFrom()));
        }

        if (this.getType().equals("All")) {
            for (final Card card : list) {
                executePayment(ability, card);
            }
            return true;
        }
        list = CardLists.getValidCards(list, this.getType().split(";"), activator, source);
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, list.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        
        InputPayment target = null;
        if (this.payCostFromSource()) {
            target = new InputExileThis(this, ability);
        } else if (this.from.equals(ZoneType.Battlefield) || this.from.equals(ZoneType.Hand)) {
            target = new InputExileType(this, this.getType(), c, ability);
        } else if (this.from.equals(ZoneType.Stack)) {
            target = new InputExileFromStack(ability, this.getType(), c, this);
        } else if (this.from.equals(ZoneType.Library)) {
            // this does not create input
            return exileFromTop(ability, c);
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
            target = new InputExileFromSame(list, this, c, payableZone);
        } else {
            target = new InputExileFrom(ability, this.getType(), c, this);
        }
        FThreads.setInputAndWait(target);
        return target.isPaid();

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
    public boolean exileFromTop(final SpellAbility sa, final int nNeeded) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Exile ").append(nNeeded).append(" cards from the top of your library?");
        final List<Card> list = sa.getActivatingPlayer().getCardsIn(ZoneType.Library, nNeeded);

        if (list.size() > nNeeded) {
            // I don't believe this is possible
            return false;
        }

        final boolean doExile = GuiDialog.confirm(sa.getSourceCard(), sb.toString());
        if (doExile) {
            final Iterator<Card> itr = list.iterator();
            while (itr.hasNext()) {
                executePayment(sa, itr.next());
            }
            return true;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        ability.getActivatingPlayer().getGame().getAction().exile(targetCard);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        // TODO Auto-generated method stub
        return "Exiled";
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(AIPlayer ai, SpellAbility ability, Card source) {
        if (this.payCostFromSource()) {
            return new PaymentDecision(source);
        } 

        if (this.getType().equals("All")) {
            return new PaymentDecision(new ArrayList<Card>(ai.getCardsIn(this.getFrom())));
        }
        
        
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        if (this.from.equals(ZoneType.Library)) {
            return new PaymentDecision(ai.getCardsIn(ZoneType.Library, c));
        } else if (this.sameZone) {
            // TODO Determine exile from same zone for AI
            return null;
        } else {
            return new PaymentDecision(ComputerUtil.chooseExileFrom(ai, this.getFrom(), this.getType(), source, ability.getTargetCard(), c));
        }
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#payAI(forge.card.cost.PaymentDecision, forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public void payAI(PaymentDecision decision, AIPlayer ai, SpellAbility ability, Card source) {
        for (final Card c : decision.cards) {
            executePayment(ability, c);
            if (this.from.equals(ZoneType.Stack)) {
                ArrayList<SpellAbility> spells = c.getSpellAbilities();
                for (SpellAbility spell : spells) {
                    if (c.isInZone(ZoneType.Exile)) {
                        final SpellAbilityStackInstance si = ai.getGame().getStack().getInstanceFromSpellAbility(spell);
                        ai.getGame().getStack().remove(si);
                    }
                }
            }
        }
    }
}
