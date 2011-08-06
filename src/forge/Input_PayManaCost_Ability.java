
package forge;

//if cost is paid, Command.execute() is called
public class Input_PayManaCost_Ability extends Input {
    private static final long serialVersionUID = 3836655722696348713L;
    
    private String            originalManaCost;
    private String            message          = "";
    private ManaCost          manaCost;

    private Command           paidCommand;
    private Command           unpaidCommand;
    
    //only used for X costs:
    private boolean 		  showOnlyOKButton = false;
    
    
    //for Abilities that don't tap
    public Input_PayManaCost_Ability(String manaCost, Command paid) {
        this(manaCost, paid, Command.Blank);
    }
    
    //Command must set InputState, by calling AllZone.Input.selectState()
    //or AllZone.Input.setState() with new InputState
    public Input_PayManaCost_Ability(String manaCost_2, Command paidCommand_2, Command unpaidCommand_2) {
        originalManaCost = manaCost_2;
        message = "";
        
        manaCost = new ManaCost(originalManaCost);
        paidCommand = paidCommand_2;
        unpaidCommand = unpaidCommand_2;
    }
    
    public Input_PayManaCost_Ability(String m, String manaCost_2, Command paidCommand_2, Command unpaidCommand_2) {
        originalManaCost = manaCost_2;
        message = m;
        
        manaCost = new ManaCost(originalManaCost);
        paidCommand = paidCommand_2;
        unpaidCommand = unpaidCommand_2;
    }
    
    public Input_PayManaCost_Ability(String m, String manaCost_2, Command paidCommand_2, Command unpaidCommand_2, boolean showOKButton) {
        originalManaCost = manaCost_2;
        message = m;
        
        manaCost = new ManaCost(originalManaCost);
        paidCommand = paidCommand_2;
        unpaidCommand = unpaidCommand_2;
        showOnlyOKButton = showOKButton;
    }
    
    
    public void resetManaCost() {
        manaCost = new ManaCost(originalManaCost);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        //only tap card if the mana is needed
        manaCost = Input_PayManaCostUtil.tapCard(card, manaCost,false);
        showMessage();
        
        if(manaCost.isPaid()) {
            resetManaCost();
            AllZone.ManaPool.clearPay(false);
            
            paidCommand.execute();
            
            AllZone.InputControl.resetInput();
        }
    }
    
    @Override
    public void selectButtonCancel() {
        resetManaCost();
        AllZone.ManaPool.unpaid();
        unpaidCommand.execute();
        AllZone.InputControl.resetInput();
    }
    
    @Override
    public void selectButtonOK() {
    	if (showOnlyOKButton)
    	{
    		unpaidCommand.execute();
    		AllZone.InputControl.resetInput();
    	}
    }
    
    @Override
    public void showMessage() {
    	ButtonUtil.enableOnlyCancel();
        if (showOnlyOKButton)
        	ButtonUtil.enableOnlyOK();
        AllZone.Display.showMessage(message + "Pay Mana Cost: \r\n" + manaCost.toString());
    }
    

}
