package forge;
public interface Computer
{
    //must shuffle this
    public Card[] getLibrary();
    
    public void main1();
    public void declare_attackers_before(); //can play Instants and Abilities
    public void declare_attackers();
    public void declare_blockers();//this is called after when the Human or Computer blocks
    public void declare_blockers_after();//can play Instants and Abilities
    public void end_of_combat();
    public void main2();
    public void end_of_turn();//end of Human's turn
    
    public void stack_not_empty();
}
