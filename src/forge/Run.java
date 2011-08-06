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
        
        //AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Dauthi Marauder", AllZone.ComputerPlayer));
        //AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Dauthi Marauder", AllZone.ComputerPlayer));
        

        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Deeptread Merrow", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Vampire Bats", AllZone.HumanPlayer));
        
        //AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Soul Feast", AllZone.HumanPlayer));
        
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
        

        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
        
        /*
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
        */
        /*
              for(int i = 0; i < 8; i++)
              {
                AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
              }
        */

//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Giant Growth", AllZone.HumanPlayer));
//    for(int i = 0; i < 9; i++)
//      AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Isochron Scepter", AllZone.HumanPlayer));
//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Isochron Scepter", AllZone.HumanPlayer));
//    AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Briarhorn", AllZone.HumanPlayer));
        
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
//    AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Bitterblossom", AllZone.ComputerPlayer));
//    AllZone.Computer_Hand.add(AllZone.CardFactory.getCard("Anaconda", AllZone.ComputerPlayer));
        
        for(int i = 0; i < 17; i++) {
            AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Forest", AllZone.ComputerPlayer));
        }
        
        //AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Elvish Piper", AllZone.ComputerPlayer));
        

        /*
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", AllZone.ComputerPlayer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", AllZone.ComputerPlayer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", AllZone.ComputerPlayer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Lantern Kami", AllZone.ComputerPlayer));
            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Garruk Wildspeaker", AllZone.ComputerPlayer));
        */


        /*
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Filthy Cur", AllZone.HumanPlayer));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Shock", AllZone.HumanPlayer));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Char", AllZone.HumanPlayer));

            AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Filthy Cur", AllZone.ComputerPlayer));
        /*
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Ancient Silverback", AllZone.HumanPlayer));
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Ancient Silverback", AllZone.HumanPlayer));
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
              AllZone.Human_Library.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));

              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Elvish Piper", AllZone.ComputerPlayer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Serra Angel", AllZone.ComputerPlayer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Birds of Paradise", AllZone.ComputerPlayer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Forest", AllZone.ComputerPlayer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Elvish Piper", AllZone.ComputerPlayer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Serra Angel", AllZone.ComputerPlayer));
              AllZone.Computer_Library.add(AllZone.CardFactory.getCard("Birds of Paradise", AllZone.ComputerPlayer));


            for(int i = 0; i < 2; i++)
            {
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.ComputerPlayer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Plains", AllZone.ComputerPlayer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.ComputerPlayer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Forest", AllZone.ComputerPlayer));
              AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Island", AllZone.ComputerPlayer));

              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
              AllZone.Human_Play.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
            }
        */

        /*

            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Angelfire Crusader", AllZone.HumanPlayer));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("March of Souls", AllZone.HumanPlayer));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Reviving Dose", AllZone.HumanPlayer));
            AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Breath of Life", AllZone.HumanPlayer));
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));

        /*
         AllZone.Human_Hand.add(AllZone.CardFactory.getCard("Nevinyrral's Disk", AllZone.HumanPlayer));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
         AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
        */
        /*
        for(int i = 0; i < 5; i++)
            AllZone.Human_Play.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
        AllZone.Human_Play.add(AllZone.CardFactory.getCard("Elvish Piper", AllZone.HumanPlayer));

        //	    AllZone.Computer_Play.add(AllZone.CardFactory.getCard("Tanglebloom", AllZone.ComputerPlayer));

           //	AllZone.Human_Play.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));


        /*
        Card c = null;
        c = AllZone.CardFactory.getCard("Hymn to Tourach", AllZone.HumanPlayer);
        AllZone.Human_Hand.add(c);


        c = AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer);
        AllZone.Human_Play.add(c);

        c = AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer);
        AllZone.Human_Play.add(c);

        /*
        c = AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer);
        AllZone.Human_Play.add(c);

        c = AllZone.CardFactory.getCard("Birds of Paradise", AllZone.HumanPlayer);
        AllZone.Human_Play.add(c);
        */

        /*
         Card c = null;
         for(int i = 0; i < 3; i++)
         {
             c = AllZone.CardFactory.getCard(s[i], AllZone.HumanPlayer);
             AllZone.Human_Hand.add(c);
         }
         c = AllZone.CardFactory.getCard("Elvish Piper", AllZone.HumanPlayer);
         AllZone.Human_Play.add(c);

             c = AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer);
         AllZone.Human_Play.add(c);

         c = AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer);
         AllZone.Human_Play.add(c);

         CardFactory cf = AllZone.CardFactory;
         for(int i = 0; i < 5; i++)
         {
            //graveyard - tests Dredge
            //AllZone.Human_Graveyard.add(cf.getCard("Darkblast", AllZone.HumanPlayer));
             AllZone.Human_Library.add(cf.getCard("Darkblast", AllZone.HumanPlayer));
         }
         AllZone.Human_Library.add(cf.getCard("Mountain", AllZone.HumanPlayer));
         AllZone.Human_Library.add(cf.getCard("Plains", AllZone.HumanPlayer));
        */
    }//main()
}