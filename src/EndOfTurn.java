//import java.util.*;

//handles "until end of turn" and "at end of turn" commands from cards
public class EndOfTurn implements java.io.Serializable
{
  private static final long serialVersionUID = -3656715295379727275L;

  private CommandList at = new CommandList();
  private CommandList until = new CommandList();

  public void addAt(Command c)    {at.add(c);}
  public void addUntil(Command c) {until.add(c);}

  public void executeAt()
  {
    //Pyrohemia and Pestilence
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());

    CardList creature = all.getType("Creature");

    if(creature.isEmpty())
    {
      CardList sacrifice = new CardList();
      sacrifice.addAll(all.getName("Pyrohemia").toArray());
      sacrifice.addAll(all.getName("Pestilence").toArray());

      for(int i = 0; i < sacrifice.size(); i++)
        AllZone.GameAction.sacrifice(sacrifice.get(i));
    }
    
    GameActionUtil.removeExaltedEffects();
    GameActionUtil.removeAttackedBlockedThisTurn();
    
    AllZone.StateBasedEffects.rePopulateStateBasedList();
    
    PlayerZone cz = AllZone.getZone(Constant.Zone.Removed_From_Play, Constant.Player.Computer);
    PlayerZone hz = AllZone.getZone(Constant.Zone.Removed_From_Play, Constant.Player.Human);
    
    CardList c = new CardList(cz.getCards());
    CardList h = new CardList(hz.getCards());
    
    System.out.println("number of cards in compy removed zone: " + c.size());
    System.out.println("number of cards in human removed zone: " + h.size());
    
    execute(at);
  }//executeAt()


  public void executeUntil() {execute(until);}

    public int sizeAt() {return at.size();}
    public int sizeUntil() {return until.size();}

  private void execute(CommandList c)
  {
    int length = c.size();

    for(int i = 0; i < length; i++)
      c.remove(0).execute();


  }
}
