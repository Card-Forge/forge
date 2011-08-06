package forge;

public class SetInfo{
	public String Code;
	public String Rarity;
	public String URL;
	
	public SetInfo()
	{
		Code = "";
		Rarity = "";
		URL = "";
	}
	
	public SetInfo(String c, String r, String u)
	{
		Code = c;
		Rarity = r;
		URL = u;
	}
	
	public SetInfo(String parse)
	{
		String[] pp = parse.split("\\|");
		Code = pp[0];
		Rarity = pp[1];
		URL = pp[2];
	}
	
	public String toString()
	{
		return Code;
	}
	
	public boolean equals(Object o)
	{
		if(o instanceof SetInfo){
			SetInfo siO = (SetInfo) o;
			return Code.equals(siO.Code);
		} else return false;
		
	}
}


