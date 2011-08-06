
package forge;


import java.util.List;


public interface Display {
    public <T> T getChoice(String message, T... choices);
    
    public <T> T getChoiceOptional(String message, T... choices);
    
    public <T> List<T> getChoices(String message, T... choices);
    
    public <T> List<T> getChoicesOptional(String message, T... choices);
    
    public void showMessage(String s);
    
    public MyButton getButtonOK();
    
    public MyButton getButtonCancel();
    
//    public void showStatus(String message);
    public void showCombat(String message);
    
    public void setVisible(boolean b);
    
    //assigns combat damage, used by Combat.setAssignedDamage()
    public void assignDamage(Card attacker, CardList blockers, int damage);
    //public void addAssignDamage(Card attacker, Card blocker, int damage);
    //public void addAssignDamage(Card attacker, int damage);

	public boolean stopAtPhase(Player turn, String phase);
}
