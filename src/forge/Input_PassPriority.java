package forge;

public class Input_PassPriority extends Input implements java.io.Serializable {
	private static final long serialVersionUID = -581477682214137181L;

	@Override
    public void showMessage() {
		GuiDisplayUtil.updateGUI();
        ButtonUtil.enableOnlyOK();
        
        String phase = AllZone.Phase.getPhase();
        Player player = AllZone.Phase.getPriorityPlayer();
        
        if (player.isComputer()){
        	System.out.println(phase + ": Computer in passpriority");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(AllZone.Phase.getPlayerTurn()).append("'s ").append(phase);
        sb.append(": ").append(player).append(" has priority for Spells or Abilities. Stack is ");
        if (AllZone.Stack.size() != 0)
        	sb.append("not ");
        
        sb.append("Empty");
        
        AllZone.Display.showMessage(sb.toString());
    }
    
    @Override
    public void selectButtonOK() {
    	AllZone.Phase.passPriority();
    	GuiDisplayUtil.updateGUI();
    	
    	if (AllZone.InputControl.getInput() == this) AllZone.InputControl.resetInput();
    	// Clear out PassPriority after clicking button
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
    	if (AllZone.GameAction.playCard(card))
    		AllZone.Phase.setPriorityPlayer(AllZone.HumanPlayer);
    }//selectCard()
}
