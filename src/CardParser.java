import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
public class CardParser {
	enum name
	{
	EXIT, MAKE, SET, GET, INPUT, ADD, PUT, IF, PAROPEN;
	public String toString(){return name().charAt(0) + name().toLowerCase().substring(1);}
	}
	HashMap<String, name> parseCommand = new HashMap<String, name>();
	public void init()//call where appropriate
	{
	  parseCommand.put("{", name.PAROPEN);
	  for (name n : name.values())
	    if(!n.toString().equals("Paropen"))
	      parseCommand.put(n.toString(), n);
	}
	static HashMap<String, Class<Object>> parseClass;
	static HashMap<Class<Object>, String> unParseClass;
	static HashMap<String, Method> parseSet;
	static HashMap<String, Method> parseGet;
	static HashMap<String, Method> parseIf;
	static HashMap<String, PlayerZone> parseZone;
	ArrayList<Object> all;
	HashMap<String, Integer> labels;	
	public Card getNextCard(BufferedReader reader) throws IOException
	{
	  labels.clear();
	  final Card res = new Card();
	  res.setName(reader.readLine());
	  all.add(res);
	  labels.put("maincard", 0);
	  parseCommand(reader, true).execute();
	  return res;
	}

	@SuppressWarnings({ "serial", "unchecked" })
	Command parseCommand(BufferedReader reader, boolean linesleft) throws IOException
	{
	  final String command = reader.readLine().trim();
	  if(command.isEmpty() || command.equals ("}"))
	  {
	    linesleft = false;
	    return Command.Blank;
	  }
	  final String[] pieces = command.split(" ");
	  switch(parseCommand.get(pieces[0])){
	    case EXIT: return null;
	    case PAROPEN:
	      boolean cont = true;
	      final CommandList todo = new CommandList();
	      while(cont) todo.add(parseCommand(reader, cont));
	      return new Command(){ public void execute()
	      {
	        for(Command c : todo)
	        {
	          if(c != null)
	            c.execute();
	          else
	            break;
	        }
	      }};
	    
		case MAKE:
	    String varname = command.split(" named ")[1];
		final int where = all.size() - 1;
	    labels.put(varname, where);
	    return new Command(){ public void execute()
	      {
	        try {
				all.set(where, parseClass.get(command.substring(5).split(" named ")[0]).newInstance());
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        //where parseClass is a Map<String, Class> containing values like <"activated ability", Ability.Class>
	      }
	    };
	    
	    //where unParseClass is a Map<Class, String> containing values like <Input.Class, "input">
	    case SET:
	    final Object destObject = fetchValue(pieces[1]);
	    final String setMethod = unParseClass.get(destObject.getClass()) + command.split(" to ")[0].substring(command.indexOf(" ", 4));
	    final Object sourceValue = fetchValue(command.split(" to ")[1]);
	    return new Command(){ public void execute(){ try {
			parseSet.get(setMethod).invoke(destObject, sourceValue);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}};
	    //where parseSet is a Map<String, Method> containing values like <"card name", Card.setName>
	    
	    case GET:
	    final Object whose_2 = fetchValue(pieces[1]);
		final String how_2 = unParseClass.get(whose_2.getClass()) + command.split(" to ")[0].substring(command.indexOf(" ", 4));
		String label2 = command.split(" named ")[1];
		final int where_2 = all.size() - 1;
	    labels.put(label2, where_2);
	    final Object what_2 = fetchValue(command.split(" to ")[1]);
	    return new Command(){ public void execute(){ try {
			all.set(where_2, parseGet.get(how_2).invoke(whose_2, what_2));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}};
	    //where parseGet is a Map<String, Method> containing values like <"spell or ability mana cost", SpellAbility.setManaCost>
	    
	    case INPUT:
	    final Object in = fetchValue(pieces[1]);
	    if(!(in instanceof Input)) throw new IllegalArgumentException("Error : " + command.split(" named ")[1] + " is not an Input");
	    return new Command(){ public void execute(){ AllZone.InputControl.setInput((Input)in);}};

	    case ADD:
	    final SpellAbility sa = (SpellAbility)fetchValue(pieces[1]);
	    final Card card = (Card)fetchValue(command.split(" to ")[1]);
	    return new Command(){ public void execute(){ card.addSpellAbility(sa);}};//TODO: Counters?
	    
	    case PUT:
	    final Card card_2 = (Card)fetchValue(pieces[1]);
	    final PlayerZone zone = parseZone.get(command.split(" in ")[1]);
	    //where parseZone is a Map<String, PLayerZone> ... i.e. <"battlefield", AllZone.Play>
	    return new Command(){ public void execute(){AllZone.GameAction.moveTo(zone, card_2);}};
	    
	    case IF:
	    final Object whose_3 = fetchValue(pieces[1]);
		final Object arg1 = fetchValue(pieces[1]);
	    final boolean not = pieces[2].equals("not");
	    final Command res = parseCommand(reader, linesleft);
	    final Method todo_2 = parseIf.get(unParseClass.get(whose_3.getClass()) + command.substring(command.indexOf(" ", 3) + (not ? 0 : 4)));
	    //where parseIf is a Map... <"input equals", Input.equals>, <"card in play", Card.inPlay>
	    Class[] need = todo_2.getParameterTypes();
	    final Object[] args = new Object[need.length];
	    if(need.length == 1) args[1] = fetchValue(pieces[pieces.length-1]);
	    return new Command(){ public void execute(){
	    	try {
				if(todo_2.invoke(arg1, args).equals(false) ^ not) res.execute();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}};
	    default:
	    	throw new IllegalArgumentException("Command not supported: " + command); 
	    }
	}
	
	Object fetchValue(String label){return fetchValue(label, Object.class);}
	@SuppressWarnings("serial")
	Object fetchValue(final String label, Class<Object> type)
	{
		if(labels.containsKey(label))
			return all.get(labels.get(label));
		if(label.contains("\""))
		{
			if(String.class.isInstance(type))
				return label.substring(1, label.length() - 1);
			if(CommandArgs.class.isInstance(type));
				return new CommandArgs(){@SuppressWarnings("unchecked")
				public void execute(Object o)
				{
					if(o instanceof ArrayList && ((ArrayList<Object>)o).size() == 2 &&	((ArrayList<Object>)o).get(0) instanceof SpellAbility && ((ArrayList<Object>)o).get(1) instanceof Card)
					{
						SpellAbility sa = ((SpellAbility)((ArrayList<Object>)o).get(0));
						Card c= ((Card)((ArrayList<Object>)o).get(1));
						CardList cl = new CardList();
						cl.add(c);
						String[] s = {label};
						if(!cl.getValidCards(s).isEmpty())//TODO: Bettter way?
							sa.setTargetCard(c);
					}
				}};
		}		
		return all.get(labels.get(label)); //i.e. throw exception
	}
}
