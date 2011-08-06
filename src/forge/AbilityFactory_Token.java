package forge;

import java.util.HashMap;

public class AbilityFactory_Token extends AbilityFactory {
	private AbilityFactory AF = null;
	
	private String tokenAmount;
	private String tokenName;
	private String[] tokenTypes;
	private String tokenOwner;
	private String[] tokenColors;
	private String[] tokenKeywords;
	private String tokenPower;
	private String tokenToughness;
	private String tokenImage;
	
	private Ability_Sub subAbAF = null;
    private boolean hasSubAbAF = false;
    private String subAbStr = "none";
    private boolean hasSubAbStr = false;
    
    public Ability_Sub getSubAbility() { return subAbAF; }
	
	public AbilityFactory_Token(final AbilityFactory af) {
		AF = af;
		
		HashMap<String,String> mapParams = af.getMapParams();
		String numTokens,numPower,numToughness,image;
		String[] keywords;
		
		if(!mapParams.get("TokenAmount").matches("[0-9][0-9]?")) //It's an X-value.
			numTokens = AF.getHostCard().getSVar(mapParams.get("TokenAmount"));
		else
			numTokens = mapParams.get("TokenAmount");
		
		if(!mapParams.get("TokenPower").matches("[0-9][0-9]?"))
			numPower = AF.getHostCard().getSVar(mapParams.get("TokenPower"));
		else
			numPower = mapParams.get("TokenPower");
		
		if(!mapParams.get("TokenToughness").matches("[0-9][0-9]?"))
			numToughness = AF.getHostCard().getSVar(mapParams.get("TokenToughness"));
		else
			numToughness = mapParams.get("TokenToughness");
		
		if(mapParams.containsKey("TokenKeywords")) {
			keywords = mapParams.get("TokenKeywords").split("<>");
		}
		else {
			keywords = new String[0];
		}
		
		if(mapParams.containsKey("TokenImage")) {
			image = mapParams.get("TokenImage");
		}
		else {
			image = "";
		}
		
		tokenAmount = numTokens;
		tokenName = mapParams.get("TokenName");
		tokenTypes = mapParams.get("TokenTypes").split(",");
		tokenOwner = mapParams.get("TokenOwner");
		tokenColors = mapParams.get("TokenColors").split(",");
		tokenPower = numPower;
		tokenToughness = numToughness;
		tokenKeywords = keywords;
		tokenImage = image;
		
		if(AF.hasSubAbility())
        {
           String sSub = AF.getMapParams().get("SubAbility");
           
           if (sSub.startsWith("SVar="))
              sSub = AF.getHostCard().getSVar(sSub.split("=")[1]);
           
           if (sSub.startsWith("DB$"))
           {
              AbilityFactory afDB = new AbilityFactory();
              subAbAF = (Ability_Sub)afDB.getAbility(sSub, AF.getHostCard());
              hasSubAbAF = true;
           }
           else
           {
              subAbStr = sSub;
              hasSubAbStr = true;
           }
        }
	}
	
	public SpellAbility getAbility()
	{
		
		
		final SpellAbility abToken = new Ability_Activated(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt())
		{
			private static final long serialVersionUID = 8460074843405764620L;

			@Override
			public boolean canPlay() {
				return super.canPlay();
			}
			
			@Override
			public boolean canPlayAI() {
				return true;
			}
			
			@Override
			public void resolve() {
				doResolve(this);
            	AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);
			}
			
			@Override
			public String getStackDescription() {
				return doStackDescription(this);
			}
		};
		
		return abToken;
	}
	
	public SpellAbility getSpell()
	{		
		final SpellAbility spToken = new Spell(AF.getHostCard(),AF.getAbCost(),AF.getAbTgt())
		{
			private static final long serialVersionUID = -8041427947613029670L;

			@Override
			public boolean canPlay() {
				return super.canPlay();
			}
			
			@Override
			public boolean canPlayAI() {
				return true;
			}
			
			@Override
			public void resolve() {
				doResolve(this);
			}
			
			@Override
			public String getStackDescription() {
				return doStackDescription(this);
			}
		};
		
		return spToken;
	}
	
	public SpellAbility getDrawback()
    {
		final SpellAbility dbDealDamage = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public String getStackDescription() {
				return doStackDescription(this);
			}

			@Override
			public void resolve() {
				doResolve(this);
			}

		}; // Spell

		return dbDealDamage;
	}
	
	private String doStackDescription(SpellAbility sa) {
		int finalPower,finalToughness,finalAmount;
		
		if(tokenPower.matches("[0-9][0-9]?")) {
			finalPower = Integer.parseInt(tokenPower);
		}
		else {
			finalPower = CardFactoryUtil.xCount(AF.getHostCard(), tokenPower);
		}
		
		if(tokenToughness.matches("[0-9][0-9]?")) {
			finalToughness = Integer.parseInt(tokenToughness);
		}
		else {
			finalToughness = CardFactoryUtil.xCount(AF.getHostCard(), tokenToughness);
		}
		
		if(tokenAmount.matches("[0-9][0-9]?")) {
			finalAmount = Integer.parseInt(tokenAmount);
		}
		else {
			finalAmount = CardFactoryUtil.xCount(AF.getHostCard(),tokenAmount);
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(AF.getHostCard().getName());
		sb.append(" - Put ").append(finalAmount).append(" ").append(finalPower).append("/").append(finalToughness).append(" ").append(tokenName).append(" tokens onto the battlefield");
		
		if(tokenOwner.equals("Opponent")) {
			sb.append(" under your opponent's control.");
		}
		else {
			sb.append(".");
		}
		
		if (hasSubAbAF){
     	   subAbAF.setParent(sa);
     	   sb.append(subAbAF.getStackDescription());
        }
		
		return sb.toString();
	}
	
	private void doResolve(SpellAbility sa) {
		String imageName = "";
		Player controller;
		String cost = "";
		//Construct colors
		String colorDesc = "";
		for(String col : tokenColors) {
			if(col.equals("White")) {
				colorDesc += "W";
			}
			else if(col.equals("Blue")) {
				colorDesc += "U";
			}
			else if(col.equals("Black")) {
				colorDesc += "B";
			}
			else if(col.equals("Red")) {
				colorDesc += "R";
			}
			else if(col.equals("Green")) {
				colorDesc += "G";
			}
			else if(col.equals("Colorless")) {
				colorDesc = "C";
			}
		}
		if(tokenImage.equals("")) {			
			
			imageName += colorDesc + " " + tokenPower + " " + tokenToughness + " " + tokenName;
		}
		else {
			imageName = tokenImage;
		}
		System.out.println("AF_Token imageName = " + imageName);
		
		for(char c : colorDesc.toCharArray()) {
			cost += c + ' ';
		}
		
		cost = colorDesc.replace('C', '1').trim();
		
		if(tokenOwner.equals("Controller")) {
			controller = AF.getHostCard().getController();
		}
		else {
			controller = AF.getHostCard().getController().getOpponent();
		}
		
		int finalPower = 0;
		int finalToughness = 0;
		int finalAmount = 0;
		
		if(tokenPower.matches("[0-9][0-9]?")) {
			finalPower = Integer.parseInt(tokenPower);
		}
		else {
			finalPower = CardFactoryUtil.xCount(AF.getHostCard(), tokenPower);
		}
		
		if(tokenToughness.matches("[0-9][0-9]?")) {
			finalToughness = Integer.parseInt(tokenToughness);
		}
		else {
			finalToughness = CardFactoryUtil.xCount(AF.getHostCard(), tokenToughness);
		}
		
		if(tokenAmount.matches("[0-9][0-9]?")) {
			finalAmount = Integer.parseInt(tokenAmount);
		}
		else {
			finalAmount = CardFactoryUtil.xCount(AF.getHostCard(),tokenAmount);
		}
		
		for(int i=0;i<finalAmount;i++) {
			CardFactoryUtil.makeToken(tokenName, imageName, controller, cost, tokenTypes, finalPower, finalToughness, tokenKeywords);
		}
		
		if (hasSubAbAF) {
     	   if (subAbAF.getParent() == null)
     		   subAbAF.setParent(sa);
			   subAbAF.resolve();
        }
		else if (hasSubAbStr){

           CardFactoryUtil.doDrawBack(subAbStr, 0, AF.getHostCard().getController(),
              AF.getHostCard().getController().getOpponent(), null, AF.getHostCard(), null, sa);
           
        }
	}
}
