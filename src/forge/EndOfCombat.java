package forge;

//handles "until end of combat" and "at end of combat" commands from cards
public class EndOfCombat implements java.io.Serializable
{

	private static final long serialVersionUID = 3035250030566186842L;

	private CommandList at = new CommandList();
	private CommandList until = new CommandList();

	public void addAt(Command c)    {at.add(c);}
	public void addUntil(Command c) {until.add(c);}

	public void executeAt()
	{
		//AllZone.StateBasedEffects.rePopulateStateBasedList();
		execute(at);
	}//executeAt()


	public void executeUntil() {
		execute(until);
	}

	public int sizeAt() {return at.size();}
	public int sizeUntil() {return until.size();}

	private void execute(CommandList c)
	{
		int length = c.size();

		for(int i = 0; i < length; i++)
			c.remove(0).execute();
	}
}
