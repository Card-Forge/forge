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
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;

/**
 * This is for the "PutCardToLib" Cost. 
 */
public class CostPutCardToLib extends CostPartWithList {
    // PutCardToLibFromHand<Num/LibPos/Type{/TypeDescription}>
    // PutCardToLibFromSameGrave<Num/LibPos/Type{/TypeDescription}>
    // PutCardToLibFromGrave<Num/LibPos/Type{/TypeDescription}>

    private ZoneType from = ZoneType.Hand;
    private boolean sameZone = false;
    private String libPosition = "0";
    
    /**
     * Gets the from.
     * 
     * @return the from
     */
    public final ZoneType getFrom() {
        return this.from;
    }
    
    /**
     * Gets the libposition.
     * 
     * @return the libposition
     */
    public final String getLibPos() {
        return this.libPosition;
    }

    /**
     * isSameZone.
     * 
     * @return a boolean
     */
    public final boolean isSameZone() {
        return this.sameZone;
    }

    /**
     * Instantiates a new cost CostPutCardToLib.
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
    public CostPutCardToLib(final String amount, final String libpos, 
            final String type, final String description, final ZoneType from) {
        super(amount, type, description);
        if (from != null) {
            this.from = from;
        }
        this.libPosition = libpos;
    }
    
    public CostPutCardToLib(final String amount, final String libpos, final String type, 
            final String description, final ZoneType from, final boolean sameZone) {
        this(amount, libpos, type, description, from);
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
        sb.append("Put ");
        
        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));

        if (this.sameZone) {
            sb.append(" from the same ");
        } else {
            sb.append(" from your ");
        }

        sb.append(this.from).append(" on ");
        
        if (this.libPosition.equals("0")) {
            sb.append("top of");
        } else {
            sb.append("the bottom of");
        }
        
        if (this.sameZone) {
            sb.append(" their owner's library");
        } else {
            sb.append(" your library");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "CardPutToLib";
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
        final Game game = activator.getGame();

        Integer i = this.convertAmount();

        if (i == null) {
            final String sVar = ability.getSVar(this.getAmount());
            if (sVar.equals("XChoice")) {
                return true;
            }
            i = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }
        
        List<Card> typeList = new ArrayList<Card>();
        if (this.sameZone) {
            typeList = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
        } else {
            typeList = new ArrayList<Card>(activator.getCardsIn(this.getFrom()));
        }

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
        
        if (typeList.size() < i) {
            return false;
        }

        if (this.sameZone) {
            boolean foundPayable = false;
            List<Player> players = game.getPlayers();
            for (Player p : players) {
                if (CardLists.filter(typeList, CardPredicates.isController(p)).size() >= i) {
                    foundPayable = true;
                    break;
                }
            }
            if (!foundPayable) {
                return false;
            }
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
    public final boolean payHuman(final SpellAbility ability, final Game game) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Card source = ability.getSourceCard();
        final Player activator = ability.getActivatingPlayer();
        
        List<Card> list;

        if (this.sameZone) {
            list = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
        } else {
            list = new ArrayList<Card>(activator.getCardsIn(this.getFrom()));
        }

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = Cost.chooseXValue(source, ability, this.getList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        
        list = CardLists.getValidCards(list, this.getType().split(";"), activator, source);
        
        if (this.from == ZoneType.Hand) {
            InputSelectCards inp = new InputSelectCardsFromList(c, c, list);
            inp.setMessage("Put %d card(s) from your " + from );
            inp.setCancelAllowed(true);
            Singletons.getControl().getInputQueue().setInputAndWait(inp);
            return !inp.hasCancelled() && executePayment(ability, inp.getSelected());
        }
        
        if (this.sameZone){
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
            return putFromSame(list, c, payableZone);
        } else {//Graveyard
            return putFromMiscZone(ability, c, list);
        }
    }
    
    /**
     * PutFromMiscZone
     * @param sa
     * @param nNeeded
     * @param typeList
     * @return a boolean
     */
    private boolean putFromMiscZone(SpellAbility sa, int nNeeded, List<Card> typeList) {
        for (int i = 0; i < nNeeded; i++) {
            if (typeList.isEmpty()) {
                return false;
            }

            final Card c = GuiChoose.oneOrNone("Put from " + getFrom() + " to library", typeList);

            if (c != null) {
                typeList.remove(c);
                executePayment(sa, c);
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean putFromSame(List<Card> list, int nNeeded, List<Player> payableZone) {
        if (nNeeded == 0) {
            return true;
        }
    
    
        final Player p = GuiChoose.oneOrNone(String.format("Put cards from whose %s?", getFrom()), payableZone);
        if (p == null) {
            return false;
        }
    
        List<Card> typeList = CardLists.filter(list, CardPredicates.isOwner(p));
    
        for (int i = 0; i < nNeeded; i++) {
            if (typeList.isEmpty()) {
                return false;
            }
    
            final Card c = GuiChoose.oneOrNone("Put cards from " + getFrom() + " to Library", typeList);
    
            if (c != null) {
                typeList.remove(c);
                executePayment(null, c);
            } else {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        targetCard.getGame().getAction().moveToLibrary(targetCard, Integer.parseInt(getLibPos()));
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        Integer c = this.convertAmount();
        final Game game = ai.getGame();
        List<Card> chosen = new ArrayList<Card>();
        List<Card> list;

        if (this.sameZone) {
            list = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
        } else {
            list = new ArrayList<Card>(ai.getCardsIn(this.getFrom()));
        }

        if (c == null) {
            final String sVar = ability.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return null;
            }
    
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        list = CardLists.getValidCards(list, this.getType().split(";"), ai, source);

        if (this.sameZone) {
            // Jotun Grunt
            // TODO: improve AI
            final List<Player> players = game.getPlayers();
            for (Player p : players) {
                List<Card> enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
                if (enoughType.size() >= c) {
                    chosen.addAll(enoughType);
                    break;
                }
            }
            chosen = chosen.subList(0, c);
        } else {
            chosen = ComputerUtil.choosePutToLibraryFrom(ai, this.getFrom(), this.getType(), source, ability.getTargetCard(), c);
        }
        return chosen.isEmpty() ? null : new PaymentDecision(chosen);
    }
}
