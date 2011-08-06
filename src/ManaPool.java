import java.util.ArrayList;
import java.util.Arrays;


public class ManaPool extends Card 
{
	private ArrayList<Ability_Mana> used = new ArrayList<Ability_Mana>();
	boolean[] spendAsCless ={true,true,true,true,true,true};
	boolean spendAll = true;
	private int cIndex(String s)
	{
		//String c =(s.length()==1 ? s : Input_PayManaCostUtil.getColor2(s)); 
		if(s.length()!=1) throw new IllegalArgumentException(s + "isn't an indexable single character.");
		if(!colors.contains(s)) return 0;
		return colors.indexOf(s) + 1;
	}
	private String indexC(int index)
	{
		if (index == 0) return "1";
		return colors.charAt(index - 1) + "";
	}
	private void updateKeywords()
	{
		extrinsicKeyword.clear();
		for(int val=0;val<6;val++)
			for(int i=0; i < has[val]; i++)
				extrinsicKeyword.add("ManaPool:" + indexC(val));
	}
	private ArrayList<String> extrinsicKeyword = new ArrayList<String>();
	public ArrayList<String> getExtrinsicKeyword() {return new ArrayList<String>(extrinsicKeyword);}
	public void setExtrinsicKeyword(ArrayList<String> a) 
	{
		extrinsicKeyword = new ArrayList<String>(a);
		Arrays.fill(has, 0);
		for(String Manaword : extrinsicKeyword)
			if (Manaword.startsWith("ManaPool:"))
			{
				String[] cost=Manaword.split(":");
				if (cost[1].length() == 1) has[cIndex(cost[1])]++;
			}
		this.updateObservers();
	}
	public void addExtrinsicKeyword(String s) 
	{
		if (s.startsWith("ManaPool:"))
		{
			extrinsicKeyword.add(s);
			addMana(s.split(":")[1]);
		}
	}
	public void removeExtrinsicKeyword(String s) 
	{
		if (s.startsWith("ManaPool:"))
		{
			updateKeywords();
			extrinsicKeyword.remove(s);
			subtractOne(s.split(":")[1]);
			this.updateObservers();
		}
	}
	public int getExtrinsicKeywordSize() {updateKeywords(); return extrinsicKeyword.size(); }
	
	public ManaPool(String contents){this(); this.addMana(contents);}
	public ManaPool()
	{
		super();
		setName("Mana Pool");
		addIntrinsicKeyword("Shroud");
		addIntrinsicKeyword("Indestructible");
		clear();
	}
	public String getText()
	{
		empty = true;
		String res="Mana available:\r\n";
		if(has[0]>0) {res+=Integer.toString(has[0]); empty = false;}
		for(int j=0; j<colors.length();j++){char c=colors.charAt(j);
			int n =has[cIndex(c+"")];
			for(int i = 0; i< n ; i++)
				res +=(c+"");
			if (n > 0) {res+="("+Integer.toString(n)+")"; empty = false;}
			}
		if (empty) res+="None";
		return res;
	}
	
	public final static String colors = "WUBRG";
	private boolean empty = false;
	private int[] paid= new int[6];
	private int[] has = new int[6];
	
	public static String oraclize(String manaCost){
		if(!manaCost.contains(" ")) return manaCost;
		String[] parts = manaCost.split(" ");
		String res="";
		for (String s : parts)
		{
			if (s.length()==2 && colors.contains(s.charAt(1) + "")) s=s.charAt(0)+"/"+s.charAt(1);
			if (s.length()==3) s="(" + s + ")";
			if (s.equals("S")) s="(S)";//for if/when we implement snow mana
			if (s.equals("X")) s="(X)";//X costs?
			res +=s;
		}		
		return res;
	}
	public ArrayList<String> getColors()
	  {
	    ArrayList<String> mana  = new ArrayList<String>();
	    for(int i = 1; i <= 5; i++)
	    {
	    	if (has[i]>0)
	    		mana.add(Input_PayManaCostUtil.getColor(colors.charAt(i-1)+""));
	    }
	    if(has[0]>0) mana.add(Constant.Color.Colorless);
	    return mana;
	  }
	public void addMana(Ability_Mana am){addMana(!am.Mana().contains("X") ? am.Mana() : am.Mana().replaceAll("X", am.getX()+""));}
	public void addMana(String mana){
		if (mana.length()<=1) {addOne(mana); return;}
		String[] cost=mana.split("");
		String Colorless = "";
		int cless = 0;
		for(String s : cost)
		{
			if(s.trim().equals("")) continue;//mana.split gave me a "" for some reason
			if(colors.contains(s))
			{ 
				has[colors.indexOf(s) + 1]++;
				if (!Colorless.trim().equals(""))
				{
					try{
						cless+= Integer.parseInt(Colorless);
						Colorless="";
					}catch(NumberFormatException ex)
					{
						throw new RuntimeException("Mana_Pool : Error, noncolor mana cost is not a number - " +Colorless);
					}
				}
			}
			else Colorless+=s;
		}
		has[0]+=cless;
	}
	public void addOne(String Mana)
	{
		if(Mana.trim().equals("")) return;
		int cInt = cIndex(Mana);
		if(cInt > 0)
			has[cInt]++;
		else try
		{
				has[cInt]+= Integer.parseInt(Mana);
		}
		catch(NumberFormatException ex)
		{
			throw new RuntimeException("Mana_Pool.AddOne : Error, noncolor mana cost is not a number - " + Mana);
		}
	}

	public static String[] getManaParts(Ability_Mana manaAbility){return getManaParts(manaAbility.Mana());}//wrapper
	public static String[] getManaParts(String Mana_2)//turns "G G" -> {"G","G"}, "2 UG"->"{"2","U/G"}, "B W U R G" -> {"B","W","U","R","G"}, etc.
	{
		String Mana=Mana_2;
		//if (Mana.isEmpty()) return null;
		if (Mana.trim().equals("")) return null;
		Mana=oraclize(Mana);
		try
		{
			String[] Colorless = {Integer.parseInt(Mana)+""};
			return Colorless;
		}
		catch(NumberFormatException ex)	{}
		
		ArrayList<String> res= new ArrayList<String>();
		int Colorless = 0;
		String clessString = "";
		boolean parentheses=false;
		String current="";
		
		for(int i=0; i<Mana.length();i++){char c=Mana.charAt(i);
			if (c=='('){parentheses=true; continue;}//Split cost handling ("(" +<W/U/B/R/G/2> + "/" + <W/U/B/R/G> + ")")
			else if(parentheses){
				if(c!=')') {current+=c; continue;}
				else {
					parentheses=false;
					res.add(current);
					current="";
					continue;
				}
			}
			String s = c+"";
			if(colors.contains(s))
			{
				res.add(s);
				if(clessString.trim().equals("")) continue;
				try
				{
					Colorless += Integer.parseInt(clessString);
				}
				catch(NumberFormatException ex)
				{
					throw new RuntimeException("Mana_Pool.getManaParts : Error, sum of noncolor mana parts is not a number - " + clessString);
				}
				clessString = "";
			}
			else clessString+=s;
		}
		if(Colorless > 0) res.add(0, Colorless+"");
		return res.toArray(new String[0]);
	}
	public ManaCost subtractMana(ManaCost m){
		spendAll = true;
		String mana = oraclize(m.toString());
		if (empty || mana.equals(null)) return m;
		if (mana.length()==1)
		{
			m=subtractOne(m,mana);
			return m;
		}
		String[] cost=getManaParts(m.toString()+"");
		for(String s : cost)
			m=subtractOne(m, s);
		return m;
	}
	public ManaCost subtarctMana(ManaCost m, Ability_Mana mability)
	{
		used.add(mability);
		for(String c : getManaParts(mability))
    	{
    		if(c.equals("")) continue; // some sort of glitch
    		subtractOne(m, c);
    	}
		return m;
	}
	public void subtractOne(String Mana){subtractOne(new ManaCost(Mana),Mana);}
	public ManaCost subtractOne(ManaCost manaCost, String Mana)
	{
		if(Mana.trim().equals("")||manaCost.toString().trim().equals("")) return manaCost;
		if(cIndex(Mana) >0 )
		{
			  if(!manaCost.isNeeded(Mana) || has[cIndex(Mana)]==0) return manaCost;
			  manaCost.subtractMana(Input_PayManaCostUtil.getColor(Mana));
			  has[cIndex(Mana)]--;
			  paid[cIndex(Mana)]++;
		}
		else 
		{
			int cless;
			try
			{
				cless = Integer.parseInt(Mana);
			}
			catch(NumberFormatException ex)
			{
				throw new RuntimeException("Mana_Pool.SubtractOne : Error, noncolor mana cost is not a number - " + Mana);
			}
			if (cless == 0) return manaCost;
			if (cless == 1)
				if(1<=has[0] && manaCost.isNeeded(Constant.Color.Colorless)){
					has[0]--;
					paid[0]++;
					manaCost.subtractMana(Constant.Color.Colorless);
					return manaCost;
				}
			if(cless>totalMana()) {manaCost=subtractOne(manaCost,totalMana()+""); return manaCost;}
			while(totalMana()>0 && cless>0 && !colors.contains(manaCost.toString().split(" ")[0]))// we're paying with colorless mana
			{
				if (has[0]>0){manaCost=subtractOne(manaCost,"1"); cless--; continue;}
				String chosen;
				String [] choices=getColors().toArray(new String[0]);
				chosen=choices[0];
				if (getColors().size()> 1)
					chosen = (String)AllZone.Display.getChoiceOptional("Choose mana to spend as colorless", choices);
				if (chosen == null) {spendAll = false; return manaCost;}
				manaCost=subtractOne(manaCost,Input_PayManaCostUtil.getColor2(chosen));
				cless--;
			}
		}
		return manaCost;
	}

	public int hasMana(String color){
		String s =(color.length()==1? color : Input_PayManaCostUtil.getColor2(color));
		Mana_Part.checkSingleMana(s);
		return(has[cIndex(s)]);
	}
	public int totalMana(){
		int res = 0;
		for (int n : has)
			res += n;
		return res;
	}
	public void clear(){
		used.clear();
		Arrays.fill(paid, 0);
		Arrays.fill(has, 0);
	}
	public void paid(){
		used.clear();
		Arrays.fill(paid, 0);
	}
	public void unpaid(){
		if (!used.isEmpty())
		{
			for (Ability_Mana am : used)
			{
				if (am.undoable())
				{
					paid[cIndex(am.Mana())]--;
					am.undo();
				}
			}
				
		}
		for(int i = 0; i < 6; i++)
			has[i]+=paid[i];
		paid();
	}

}