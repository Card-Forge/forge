package forge.card.cost;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

public class CostSacrifice extends CostPartWithList {

	public CostSacrifice(String amount, String type, String description){
	    super(amount, type, description);
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("Sacrifice ");

        Integer i = convertAmount();
        
        if (getThis())
            sb.append(type);
        else{
        	String desc = typeDescription == null ? type : typeDescription;
	        if (i != null)
	        	sb.append(Cost.convertIntAndTypeToWords(i, desc));
	        else
	        	sb.append(Cost.convertAmountTypeToWords(amount, desc));
        }
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        // You can always sac all
        if (!getThis()) {
            CardList typeList = activator.getCardsIn(Zone.Battlefield);
            typeList = typeList.getValidCards(getType().split(";"), activator, source);

            Integer amount = convertAmount();   
            
            if (amount != null && typeList.size() < amount)
                return false;
            
            // If amount is null, it's either "ALL" or "X" 
            // if X is defined, it needs to be calculated and checked, if X is choice, it can be Paid even if it's 0 
        } else if (!AllZoneUtil.isCardInPlay(source))
            return false;
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().sacrifice(c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        String type = getType();
        Player activator = ability.getActivatingPlayer();
        CardList list = activator.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(type.split(";"), activator, source);
        
        if (getThis()){
            CostUtil.setInput(CostSacrifice.sacrificeThis(ability, payment, this));
        }
        else if (amount.equals("All")){
            this.list = list;
            CostSacrifice.sacrificeAll(ability, payment, this, list);
            addListToHash(ability, "Sacrificed");
            return true;
        }
        else{
            Integer c = convertAmount();
            if (c == null){
                String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")){
                    c = CostUtil.chooseXValue(source, list.size());
                }
                else{
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }
            
            CostUtil.setInput(CostSacrifice.sacrificeFromList(ability, payment, this, list, c));
        }
            
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        resetList();
        Player activator = ability.getActivatingPlayer();
        if (getThis()){
            list.add(source);
        }
        else if (amount.equals("All")) {
            CardList typeList = activator.getCardsIn(Zone.Battlefield);
            typeList = typeList.getValidCards(type.split(","), activator, source);
            // Does the AI want to use Sacrifice All?
            return false;
        } else{
            Integer c = convertAmount();
            if (c == null){
                if (source.getSVar(amount).equals("XChoice")){
                    return false;
                }
                
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
            list = ComputerUtil.chooseSacrificeType(type, source, ability.getTargetCard(), c);
            if (list == null)
                return false;
        }
        return true;
    }

    // Inputs
    
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
     * <p>sacrificeFromList.</p>
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
                if (nNeeded == 0){
                    done();
                }
                
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
                    part.addToList(card);
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
                part.addListToHash(sa, "Sacrificed");
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
     * <p>sacrificeThis.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param part TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeThis(final SpellAbility sa, final Cost_Payment payment, final CostSacrifice part) {
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
                        part.addToList(card);
                        part.addListToHash(sa, "Sacrificed");
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
}
