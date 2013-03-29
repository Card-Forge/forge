package forge.control.input;

import forge.Card;

public abstract class InputSelectCards extends InputSelectManyBase<Card> {
    private static final long serialVersionUID = -6609493252672573139L;

    protected InputSelectCards(int min, int max) {

        super(min, max);
    }

    @Override
    public final void selectCard(final Card c) {
        selectEntity(c);
    }
    
    /* (non-Javadoc)
     * @see forge.control.input.InputSelectListBase#onSelectStateChanged(forge.GameEntity, boolean)
     */
    @Override
    protected void onSelectStateChanged(Card c, boolean newState) {
        c.setUsedToPay(newState); // UI supports card highlighting though this abstraction-breaking mechanism
    } 
    
    /* (non-Javadoc)
     * @see forge.control.input.InputSyncronizedBase#afterStop()
     */
    @Override
    protected void afterStop() {
         for(Card c : selected)
             c.setUsedToPay(false);
         super.afterStop(); // It's ultimatelly important to keep call to super class!
     
    }
    
}
