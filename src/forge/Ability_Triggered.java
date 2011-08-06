
package forge;


import java.util.Arrays;


public class Ability_Triggered extends Ability implements Command {
    

    /**
	 * 
	 */
    private static final long serialVersionUID = 4970998845621323960L;
    
    public String[]           restrictions;
    public ZCTrigger          trigger;
    public Command            todo;
    
    public Ability_Triggered(Card sourceCard, Command sourceCommand, String situation) {
        this(sourceCard, sourceCommand, ZCTrigger.getTrigger(situation));
    }
    
    public Ability_Triggered(Card sourceCard, Command sourceCommand, ZCTrigger situation) {
        super(sourceCard, "no cost");
        todo = sourceCommand;
        trigger = situation;
        if(todo instanceof Ability_Triggered) {
            setStackDescription(((SpellAbility) todo).getStackDescription());
            restrictions = ((Ability_Triggered) todo).restrictions;
        } else {
            setStackDescription("Triggered ability: " + sourceCard + " " + situation);
            restrictions = new String[] {"named " + sourceCard.getName()};
        }
    }
    
    @Override
    public boolean canPlay() {
        return false;
    }//this is a triggerred ability: it cannot be "played"
    
    @Override
    public void resolve() {
        todo.execute();
    }
    
    public void execute() {
        resolve();
    }
    
    public boolean triggerFor(Card c) {
        return !(new CardList(c)).getValidCards(restrictions,c.getController(),c).isEmpty();
    }
    
    public boolean triggerOnZoneChange(String sourceZone, String destinationZone) {
        return trigger.triggerOn(sourceZone, destinationZone);
    }
    
    @Override
    public boolean equals(Object o)//TODO: triggers affecting other cards
    {
        if(!(o instanceof Ability_Triggered)) return false;
        Ability_Triggered tmp = (Ability_Triggered) o;
        return tmp.getSourceCard().equals(getSourceCard()) && tmp.trigger.equals(trigger) && tmp.todo.equals(todo)
                && Arrays.equals(tmp.restrictions, restrictions);
    }
    
    public boolean isBasic() {
        return restrictions.length == 1 && restrictions[0].equals("named " + getSourceCard().getName());
    }
}
