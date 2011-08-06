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
        
        sb.append("Turn : ").append(AllZone.Phase.getPlayerTurn()).append("\n");
        sb.append("Phase: ").append(phase).append("\n");
        sb.append("Stack: ");
        if (AllZone.Stack.size() != 0)
        	sb.append(AllZone.Stack.size()).append(" to Resolve.");
        else
        	sb.append("Empty");
        sb.append("\n");
        sb.append("Priority: ").append(player);

        AllZone.Display.showMessage(sb.toString());
    }
    
    @Override
    public void selectButtonOK() {
    	AllZone.Phase.passPriority();
    	GuiDisplayUtil.updateGUI();
    	Input in = AllZone.InputControl.getInput();
    	if (in == this || in == null) 
    		AllZone.InputControl.resetInput();
    	// Clear out PassPriority after clicking button
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
    	if (AllZone.GameAction.playCard(card))
    		AllZone.Phase.setPriorityPlayer(AllZone.HumanPlayer);
    }//selectCard()
}
