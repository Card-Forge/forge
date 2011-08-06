
package forge;


public class Input_NoCost_TapAbility extends Input {
    private static final long serialVersionUID = 5382228961284472286L;
    
    private final Ability_Tap ability;
    
    public Input_NoCost_TapAbility(Ability_Tap ab) {
        ability = ab;
    }
    
    @Override
    public void showMessage() {
        //prevents this from running multiple times, which it is for some reason
        if(ability.getSourceCard().isUntapped()) {
            ability.getSourceCard().tap();
            AllZone.Stack.add(ability);
            stopSetNext(new ComputerAI_StackNotEmpty());
        }
    }
}
