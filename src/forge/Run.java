package forge;
public class Run {
    public static void main(String[] args) {
        AllZone.Computer = new ComputerAI_Input(new ComputerAI_General());
        

        //AllZone.Display = new GuiDisplay2();
        
        Deck human = new Deck(Constant.GameType.Constructed);
        Deck computer = new Deck(Constant.GameType.Constructed);
        
        for(int i = 0; i < 12; i++) {
//      human.addMain("Plains");
//      computer.addMain("Plains");
        }
        
        AllZone.GameAction.newGame(human, computer);
        AllZone.Display.setVisible(true);
        
        //***************************************************
        //testing purposes
        
        //CardFactory cf = AllZone.CardFactory;
        
        //AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Dauthi Marauder", Constant.Player.Computer));
        //AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Dauthi Marauder", Constant.Player.Computer));
        

        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Deeptread Merrow", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Vampire Bats", Constant.Player.Human));
        
        //AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Soul Feast", Constant.Player.Human));
        
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Human));
        

        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
        
        /*
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Human));
        */
        /*
              for(int i = 0; i < 8; i++)
              {
                AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
              }
        */

//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Giant Growth", Constant.Player.Human));
//    for(int i = 0; i < 9; i++)
//      AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Human));
//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Isochron Scepter", Constant.Player.Human));
//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Isochron Scepter", Constant.Player.Human));
//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Briarhorn", Constant.Player.Human));
        
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
//    AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Bitterblossom", Constant.Player.Computer));
//    AllZone.Computer_Hand.add(AllZone.CardFactory.getCard("Anaconda", Constant.Player.Computer));
        
        for(int i = 0; i < 17; i++) {
            AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Computer));
        }
        
        //AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Elvish Piper", Constant.Player.Computer));
        

        /*
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", Constant.Player.Computer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", Constant.Player.Computer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", Constant.Player.Computer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", Constant.Player.Computer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Garruk Wildspeaker", Constant.Player.Computer));
        */


        /*
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Filthy Cur", Constant.Player.Human));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Shock", Constant.Player.Human));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Char", Constant.Player.Human));

            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Filthy Cur", Constant.Player.Computer));
        /*
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Ancient Silverback", Constant.Player.Human));
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Ancient Silverback", Constant.Player.Human));
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Human));
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Human));

              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Elvish Piper", Constant.Player.Computer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Serra Angel", Constant.Player.Computer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Birds of Paradise", Constant.Player.Computer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Computer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Elvish Piper", Constant.Player.Computer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Serra Angel", Constant.Player.Computer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Birds of Paradise", Constant.Player.Computer));


            for(int i = 0; i < 2; i++)
            {
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Computer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Computer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Computer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Computer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Island", Constant.Player.Computer));

              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Human));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", Constant.Player.Human));
            }
        */

        /*

            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Angelfire Crusader", Constant.Player.Human));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("March of Souls", Constant.Player.Human));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Reviving Dose", Constant.Player.Human));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Breath of Life", Constant.Player.Human));
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Human));

        /*
         AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Nevinyrral's Disk", Constant.Player.Human));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));
        */
        /*
        for(int i = 0; i < 5; i++)
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Forest", Constant.Player.Human));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Elvish Piper", Constant.Player.Human));

        //	    AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Tanglebloom", Constant.Player.Computer));

           //	AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", Constant.Player.Human));


        /*
        Card c = null;
        c = AllZone.CardFactory.getCard("Hymn to Tourach", Constant.Player.Human);
        AllZone.Human_Hand.add(c);


        c = AllZone.CardFactory.getCard("Swamp", Constant.Player.Human);
        AllZone.Human_Play.add(c);

        c = AllZone.CardFactory.getCard("Swamp", Constant.Player.Human);
        AllZone.Human_Play.add(c);

        /*
        c = AllZone.CardFactory.getCard("Forest", Constant.Player.Human);
        AllZone.Human_Play.add(c);

        c = AllZone.CardFactory.getCard("Birds of Paradise", Constant.Player.Human);
        AllZone.Human_Play.add(c);
        */

        /*
         Card c = null;
         for(int i = 0; i < 3; i++)
         {
             c = AllZone.CardFactory.getCard(s[i], Constant.Player.Human);
             AllZone.Human_Hand.add(c);
         }
         c = AllZone.CardFactory.getCard("Elvish Piper", Constant.Player.Human);
         AllZone.Human_Play.add(c);

             c = AllZone.CardFactory.getCard("Swamp", Constant.Player.Human);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", Constant.Player.Human);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", Constant.Player.Human);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", Constant.Player.Human);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", Constant.Player.Human);
         AllZone.Human_Play.add(c);

         CardFactory cf = AllZone.CardFactory;
         for(int i = 0; i < 5; i++)
         {
            //graveyard - tests Dredge
            //AllZone.Human_Graveyard.add(cf.getCard("Darkblast", Constant.Player.Human));
             AllZone.Human_Library.add(cf.getCard("Darkblast", Constant.Player.Human));
         }
         AllZone.Human_Library.add(cf.getCard("Mountain", Constant.Player.Human));
         AllZone.Human_Library.add(cf.getCard("Plains", Constant.Player.Human));
        */
    }//main()
}