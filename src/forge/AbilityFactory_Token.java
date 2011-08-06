package forge;

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
	
	public SpellAbility getAbility(final AbilityFactory af,final String numTokens,final String name,final String[] types,final String owner,final String[] colors,final String power,final String toughness,final String[] keywords,final String image)
	{
		AF = af;
		tokenAmount = numTokens;
		tokenName = name;
		tokenTypes = types;
		tokenOwner = owner;
		tokenColors = colors;
		tokenPower = power;
		tokenToughness = toughness;
		tokenKeywords = keywords;
		tokenImage = image;
		
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
				return doStackDescription();
			}
		};
		
		return abToken;
	}
	
	public SpellAbility getSpell(final AbilityFactory af,final String numTokens,final String name,final String[] types,final String owner,final String[] colors,final String power,final String toughness,final String[] keywords,final String image)
	{
		AF = af;
		tokenAmount = numTokens;
		tokenName = name;
		tokenTypes = types;
		tokenOwner = owner;
		tokenColors = colors;
		tokenPower = power;
		tokenToughness = toughness;
		tokenKeywords = keywords;
		tokenImage = image;
		
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
				return doStackDescription();
			}
		};
		
		return spToken;
	}
	
	private String doStackDescription() {
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
		
	}
}
