package forge;

public class SetInfo{
	public String Code;
	public String Rarity;
	public String URL;
	public int PicCount;
	
	public SetInfo()
	{
		Code = "";
		Rarity = "";
		URL = "";
		PicCount = 0;
	}
	
	public SetInfo(String c, String r, String u)
	{
		Code = c;
		Rarity = r;
		URL = u;
		PicCount = 0;
	}
	
	public SetInfo(String c, String r, String u, int p)
	{
		Code = c;
		Rarity = r;
		URL = u;
		PicCount = p;
	}
	
	public SetInfo(String parse)
	{
		String[] pp = parse.split("\\|");
		Code = pp[0];
		Rarity = pp[1];
		URL = pp[2];
		if (pp.length > 3)
			PicCount = Integer.parseInt(pp[3]);
		else
			PicCount = 0;
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


