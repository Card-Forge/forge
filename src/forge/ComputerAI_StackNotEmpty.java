
package forge;


public class ComputerAI_StackNotEmpty extends Input {
    private static final long serialVersionUID = -3995969852275185353L;
    
    @Override
    public void showMessage() {
    	/*
        AllZone.Display.showMessage("Phase: "
                + AllZone.Phase.getPhase()
                + "\nAn error may have occurred. Please send the \"Stack Report\" and the \"Detailed Error Trace\" to the Forge forum.");
        */
        AllZone.Computer.stackNotEmpty();
    }
}
