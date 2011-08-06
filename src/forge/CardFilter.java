package forge;

public class CardFilter {
	
	
	public CardList CardListNameFilter(CardList all, String name)
	{
	Card CardName; 
	String s;
	s="";
	CardList listFilter = new CardList();
		for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);
			s=CardName.getName().toLowerCase();
			
			if(s.indexOf(name.toLowerCase())>=0)
			{
				listFilter.add(CardName);
				
			}
			
		}
		
		return listFilter;
	}
	
	public CardList CardListTextFilter(CardList all, String name)
	{
	Card CardName; 
	String s;
	s="";
	CardList listFilter = new CardList();
		for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);
			s=CardName.getText().toLowerCase();
			
			if(s.indexOf(name.toLowerCase())>=0)
			{
				listFilter.add(CardName);
				
			}
			
		}
		
		return listFilter;
	}
	
	
	public CardList CardListColorFilter(CardList all, String name)
	{
	Card CardName = new Card(); 
	CardList listFilter = new CardList();
	
		if(name=="black"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardUtil.getColors(CardName).contains(Constant.Color.Black)==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="blue"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardUtil.getColors(CardName).contains(Constant.Color.Blue)==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="green"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardUtil.getColors(CardName).contains(Constant.Color.Green)==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="red"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardUtil.getColors(CardName).contains(Constant.Color.Red)==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="white"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardUtil.getColors(CardName).contains(Constant.Color.White)==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name.equals("colorless")){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardUtil.getColors(CardName).contains(Constant.Color.Colorless)==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		return listFilter;
	}
	
	public CardList CardListTypeFilter(CardList all, String name)
	{
	Card CardName = new Card(); 
	CardList listFilter = new CardList();
	
		if(name=="artifact"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isArtifact()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="creature"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isCreature()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="enchantment"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isEnchantment()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="instant"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isInstant()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="land"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isLand()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name=="planeswalker"){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isPlaneswalker()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		if(name.equals("sorcery")){
			for(int i=0;i<all.size();i++){
			CardName = all.getCard(i);		
			if(CardName.isSorcery()==false)
			{
				listFilter.add(CardName);
			}
			
		}}
		
		return listFilter;
	}
	
	

}
