package forge;
public class All
{
    public Phase Phase = new Phase();
    public MagicStack Stack = new MagicStack();

    //public  PlayerLife Human_Life    = new PlayerLife(AllZone.HumanPlayer);
    //public  PlayerLife ComputerPlayer = new PlayerLife(AllZone.ComputerPlayer);

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
    public PlayerZone Human_Play      = new PlayerZone_ComesIntoPlay(Constant.Zone.Play, AllZone.HumanPlayer);
    public PlayerZone Human_Hand      = new DefaultPlayerZone(Constant.Zone.Hand      , AllZone.HumanPlayer);
    public PlayerZone Human_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard , AllZone.HumanPlayer);
    public PlayerZone Human_Library   = new DefaultPlayerZone(Constant.Zone.Library   , AllZone.HumanPlayer);
    public PlayerZone Human_Removed   = new DefaultPlayerZone(Constant.Zone.Removed_From_Play, AllZone.HumanPlayer);

    public PlayerZone Computer_Play      = new PlayerZone_ComesIntoPlay(Constant.Zone.Play      , AllZone.ComputerPlayer);
    public PlayerZone Computer_Hand      = new DefaultPlayerZone(Constant.Zone.Hand      , AllZone.ComputerPlayer);
    public PlayerZone Computer_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard , AllZone.ComputerPlayer);
    public PlayerZone Computer_Library   = new DefaultPlayerZone(Constant.Zone.Library   , AllZone.ComputerPlayer);
    public PlayerZone Computer_Removed   = new DefaultPlayerZone(Constant.Zone.Removed_From_Play, AllZone.ComputerPlayer);
}