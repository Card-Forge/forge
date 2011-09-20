package forge.card.cost;

import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;

public class CostUntap extends CostPart {
	public CostUntap(){
		isReusable = true;
		isUndoable = true;
	}

	@Override
	public String toString() {
		return "Untap";
	}

	@Override
	public void refund(Card source) {
		source.tap();
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        return source.isTapped() && (!source.hasSickness() || !source.isCreature());
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        source.untap();
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        //if (!canPay(ability, source, ability.getActivatingPlayer(), payment.getCost()))
        //    return false;
        
        source.untap();
        payment.setPaidManaPart(this, true);
        return true;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        return true;
    }
}
