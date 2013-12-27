package forge.game.cost;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CostExileAndPay extends CostPartWithList {

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#doPayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        targetCard.getGame().getAction().exile(targetCard);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "Exiled";
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPay(SpellAbility ability) {
        return CardLists.getValidCards(ability.getActivatingPlayer().getZone(ZoneType.Graveyard), "Creature", ability.getActivatingPlayer(), ability.getSourceCard()).size() > 0;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        List<Card> validGrave = CardLists.getValidCards(ability.getActivatingPlayer().getZone(ZoneType.Graveyard), "Creature", ability.getActivatingPlayer(), ability.getSourceCard());

        if(validGrave.size() == 0)
        {
            return null;
        }
        
        Card bestCard = null;
        int bestScore = 0;
        
        for(Card candidate : validGrave)
        {
            boolean selectable = false;
            for(SpellAbility sa : candidate.getSpellAbilities())
            {
                if(sa instanceof SpellPermanent)
                {
                    if(ComputerUtilCost.canPayCost(sa, ai))
                    {
                        selectable = true;
                    }
                }
            }
            
            if(!selectable)
            {
                continue;
            }
            
            int candidateScore = ComputerUtilCard.evaluateCreature(candidate);
            if(candidateScore > bestScore)
            {
                bestScore = candidateScore;
                bestCard = candidate;
            }
        }
        
        return bestCard == null ? null : new PaymentDecision(bestCard);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility, forge.game.GameState)
     */
    @Override
    public boolean payHuman(SpellAbility ability, Player payer) {
        List<Card> validGrave = CardLists.getValidCards(payer.getZone(ZoneType.Graveyard), "Creature", payer, ability.getSourceCard());
        
        Card selectedCard = GuiChoose.oneOrNone("Choose a creature card to exile.", validGrave);
        if(selectedCard == null)
        {
            return false;
        }
        
        List<Cost> options = new ArrayList<Cost>();
        for(SpellAbility sa : selectedCard.getSpellAbilities())
        {
            if(sa instanceof SpellPermanent)
            {
                options.add(sa.getPayCosts());
            }
        }
        
        Cost selectedCost;
        if(options.size() > 1)
        {
            selectedCost = GuiChoose.oneOrNone("Choose a cost", options);
        }
        else
        {
            selectedCost = options.get(0);
        }
        
        if(selectedCost == null)
        {
            return false;
        }
        
        final CostPayment pay = new CostPayment(selectedCost, ability);
        pay.payCost(payer);
        if(!pay.isFullyPaid())
        {
            return false;
        }
        
        executePayment(ability,selectedCard);
        
        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#payAI(forge.card.cost.PaymentDecision, forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public boolean payAI(PaymentDecision decision, Player ai, SpellAbility ability, Card source) {
        for (final Card c : decision.cards) {
            executePayment(ability, c);
            for(SpellAbility sa : c.getSpellAbilities())
            {
                if(sa instanceof SpellPermanent && ComputerUtilCost.canPayCost(sa, ai))
                {
                    final CostPayment pay = new CostPayment(sa.getPayCosts(), sa);
                    pay.payComputerCosts(ai, ai.getGame());
                }
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public String toString() {
        return "Exile a creature card from your graveyard and pay it's mana cost";
    }
    
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
