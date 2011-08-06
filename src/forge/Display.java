
package forge;




public interface Display {    
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
    
    public boolean loadPrefs();
    
    public boolean savePrefs();

	public boolean canLoseByDecking();
	
	public void setCard(Card c);
}
