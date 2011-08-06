package forge;

import java.util.ArrayList;
//import java.util.*;

//handles "until end of turn" and "at end of turn" commands from cards
public class EndOfTurn implements java.io.Serializable
{
  private static final long serialVersionUID = -3656715295379727275L;

  private CommandList at = new CommandList();
  private CommandList until = new CommandList();
  private CommandList last = new CommandList();

  public void addAt(Command c)    {at.add(c);}
  public void addUntil(Command c) {until.add(c);}
  public void addLast(Command c) {last.add(c);}

  public void executeAt()
  {
	  // Whenever Keyword
		CardList Cards_In_Play = new CardList();
		Cards_In_Play.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
		Cards_In_Play.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
		Cards_In_Play = Cards_In_Play.filter(new CardListFilter() {
           public boolean addCard(Card c) {
               if(c.getKeyword().toString().contains("WheneverKeyword")) return true;
               return false;
           }
       });
		boolean Triggered = false;
		for(int i = 0; i < Cards_In_Play.size() ; i++) {	
		if(Triggered == false) {	
			Card card = Cards_In_Play.get(i);
		        ArrayList<String> a = card.getKeyword();
		        int WheneverKeywords = 0;
		        int WheneverKeyword_Number[] = new int[a.size()];
		        for(int x = 0; x < a.size(); x++)
		            if(a.get(x).toString().startsWith("WheneverKeyword")) {
		            	WheneverKeyword_Number[WheneverKeywords] = x;
		            	WheneverKeywords = WheneverKeywords + 1;
		            }
		        for(int CKeywords = 0; CKeywords < WheneverKeywords; CKeywords++) {
               String parse = card.getKeyword().get(WheneverKeyword_Number[CKeywords]).toString();                
               String k[] = parse.split(":");
               if((k[1].equals("BeginningOfEndStep"))) {
              	 AllZone.GameAction.RunWheneverKeyword(card, "BeginningOfEndStep"); // Beached
              	 Triggered = true;
               }
		        }
		}
		}
		  // Whenever Keyword
	  
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
    
    GameActionUtil.endOfTurn_Predatory_Advantage();
    GameActionUtil.endOfTurn_Wall_Of_Reverence();
    GameActionUtil.endOfTurn_Lighthouse_Chronologist();
    GameActionUtil.endOfTurn_Thran_Quarry();
    GameActionUtil.endOfTurn_Glimmervoid();
    
    //GameActionUtil.removeExaltedEffects();
    GameActionUtil.removeAttackedBlockedThisTurn();
    AllZone.GameInfo.setPreventCombatDamageThisTurn(false);
    
    AllZone.StaticEffects.rePopulateStateBasedList();
    
    /*
    PlayerZone cz = AllZone.getZone(Constant.Zone.Removed_From_Play, Constant.Player.Computer);
    PlayerZone hz = AllZone.getZone(Constant.Zone.Removed_From_Play, Constant.Player.Human);
    
    CardList c = new CardList(cz.getCards());
    CardList h = new CardList(hz.getCards());
    
    System.out.println("number of cards in compy removed zone: " + c.size());
    System.out.println("number of cards in human removed zone: " + h.size());
    */
    for(Card c : all)
      if(!c.isFaceDown()
    	&& c.getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME."))
      {
    	  final Card card = c;
    	  final SpellAbility sac = new Ability(card, "0") {
              @Override
              public void resolve() {
                  if(AllZone.GameAction.isCardInPlay(card)) AllZone.GameAction.sacrifice(card);
              }
          };
          sac.setStackDescription("Sacrifice " + card);
    	  AllZone.Stack.add(sac);
      }
    execute(at);
  }//executeAt()


  public void executeUntil() {
	  execute(until);
	  execute(last);
  }

    public int sizeAt() {return at.size();}
    public int sizeUntil() {return until.size();}
    public int sizeLast() { return last.size();}

  private void execute(CommandList c)
  {
    int length = c.size();

    for(int i = 0; i < length; i++)
      c.remove(0).execute();
  }
}
