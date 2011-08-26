package forge.card.cost;

import java.util.Iterator;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.GameActionUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

public class CostExile extends CostPartWithList {
	//Exile<Num/Type{/TypeDescription}>
	//ExileFromHand<Num/Type{/TypeDescription}>
	//ExileFromGraveyard<Num/Type{/TypeDescription}>
	//ExileFromTop<Num/Type{/TypeDescription}> (of library)

    private String from = Constant.Zone.Battlefield;

    public String getFrom() {
        return from;
    }
    
    public CostExile(String amount, String type, String description, String from){
        super(amount, type, description);
    	if (from != null)
    		this.from = from;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("Exile ");

        if (getThis()) {
        	sb.append(type);
        }
        else{
	        Integer i = convertAmount();
	        String desc = typeDescription == null ? type : typeDescription;
	        
	        sb.append(Cost.convertAmountTypeToWords(i, amount, desc));
        }
        
        if (from.equals("Battlefield")){
        	if (!getThis())
        		sb.append(" you control");
        }
        else{
        	sb.append(" from your ").append(from);
        }
        
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        PlayerZone zone = AllZone.getZone(getFrom(), activator);
        if (!getThis()) {
            CardList typeList = AllZoneUtil.getCardsInZone(zone);

            typeList = typeList.getValidCards(getType().split(";"), activator, source);
            
            Integer amount = convertAmount(); 
            if (amount != null && typeList.size() < amount)
                return false;
        } else if (!AllZoneUtil.isCardInZone(zone, source))
            return false;
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().exile(c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        Integer c = convertAmount();
        Player activator = ability.getActivatingPlayer();
        CardList list = AllZoneUtil.getCardsInZone(getFrom(), activator);
        list = list.getValidCards(type.split(";"), activator, source);
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
        if (getThis()){
            CostUtil.setInput(CostExile.exileThis(ability, payment, this));
        }
        else if (from.equals(Constant.Zone.Battlefield) || from.equals(Constant.Zone.Hand)){
            CostUtil.setInput(CostExile.exileType(ability, this, getType(), payment, c));
        }
        else if (from.equals(Constant.Zone.Library)){
            CostExile.exileFromTop(ability, this, payment, c);
        }
        else{
            CostUtil.setInput(CostExile.exileFrom(ability, this, getType(), payment, c));
        }
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        resetList();
        if (getThis())
            list.add(source);
        else{
            Integer c = convertAmount();
            if (c == null){
                String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")){
                    return false;
                }
                
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
            
            if (from.equals(Constant.Zone.Library)){
                list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getComputerPlayer(), c);
            }
            else{
                list = ComputerUtil.chooseExileFrom(getFrom(), getType(), source, ability.getTargetCard(), c);
            }
            if (list == null || list.size() < c)
                return false;
        }
        return true;
    }
    
    // Inputs
    
    public static void exileFromTop(final SpellAbility sa, final CostExile part, final Cost_Payment payment, final int nNeeded){
        StringBuilder sb = new StringBuilder();
        sb.append("Exile ").append(nNeeded).append(" cards from the top of your library?");
        CardList list = AllZoneUtil.getPlayerCardsInLibrary(sa.getActivatingPlayer(), nNeeded);
        
        if (list.size() > nNeeded){
            // I don't believe this is possible
            payment.cancelCost();
            return;
        }
        
        boolean doExile = GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString());
        if (doExile){
            Iterator<Card> itr = list.iterator();
            while(itr.hasNext()){
                Card c = (Card)itr.next();
                part.addToList(c);
                AllZone.getGameAction().exile(c);
            }
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        }
        else{
            payment.cancelCost();
        }
    }

    public static Input exileFrom(final SpellAbility sa, final CostExile part, final String type, final Cost_Payment payment, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 734256837615635021L;
            CardList typeList;
    
            @Override
            public void showMessage() {
                if (nNeeded == 0){
                    done();
                }
                
                typeList = AllZoneUtil.getCardsInZone(part.getFrom(), sa.getActivatingPlayer());
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
    
                for (int i = 0; i < nNeeded; i++) {
                    if (typeList.size() == 0)
                        cancel();
    
                    Object o = GuiUtils.getChoiceOptional("Exile from "+part.getFrom(), typeList.toArray());
    
                    if (o != null) {
                        Card c = (Card) o;
                        typeList.remove(c);
                        part.addToList(c);
                        AllZone.getGameAction().exile(c);
                        if (i == nNeeded - 1) done();
                    }
                    else{
                        cancel();
                        break;
                    }
                }
            }
    
            @Override
            public void selectButtonCancel() {
                cancel();
            }
    
            public void done() {
                stop();
                part.addListToHash(sa, "Exiled");
                payment.paidCost(part);
            }
    
            public void cancel() {
                stop();
                payment.cancelCost();
            }
        };
        return target;
    }//exileFrom()

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
                if (nNeeded == 0){
                    done();
                }
                
                StringBuilder msg = new StringBuilder("Exile ");
                int nLeft = nNeeded - nExiles;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }
                
                if (part.getFrom().equals(Constant.Zone.Hand)){
                    msg.append(" from your Hand");
                }
                typeList = AllZoneUtil.getCardsInZone(part.getFrom(), sa.getActivatingPlayer());
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
                    part.addToList(card);
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
                part.addListToHash(sa, "Exiled");
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
                        part.addToList(card);
                        stop();
                        part.addListToHash(sa, "Exiled");
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
}
