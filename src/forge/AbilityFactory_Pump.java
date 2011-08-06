package forge;

import java.util.HashMap;

public class AbilityFactory_Pump {
	
	private final int NumAttack[] = {-1138};
	private final int NumDefense[] = {-1138};
	private final String AttackX[] = {"none"};
	private final String DefenseX[] = {"none"};

	private AbilityFactory AF = null;
	private HashMap<String,String> params = null;
	private Card hostCard = null;
	
	public AbilityFactory_Pump (AbilityFactory newAF){
		AF = newAF;
		
		params = AF.getMapParams();
		
		hostCard = AF.getHostCard();
		
		if (params.containsKey("NumAtt"))
		{
			String tmp = params.get("NumAtt");
            if(tmp.matches("[\\+\\-][XY]"))
            {
                String xy = hostCard.getSVar(tmp.replaceAll("[\\+\\-]", ""));
                if(xy.startsWith("Count$")) {
                    String kk[] = xy.split("\\$");
                    AttackX[0] = kk[1];
                    
                    if(tmp.contains("-"))
                    {
                    	if(AttackX[0].contains("/"))
                    		AttackX[0] = AttackX[0].replace("/", "/Negative");
                    	else 
                    		AttackX[0] += "/Negative";
                    }
                }
            } 
            else if(tmp.matches("[\\+\\-][0-9]"))
            	NumAttack[0] = Integer.parseInt(tmp.replace("+", ""));
            

		}
	}
}
