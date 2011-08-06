public class All
{
    public Phase Phase = new Phase();
    public MagicStack Stack = new MagicStack();

    public  PlayerLife Human_Life    = new PlayerLife();
    public  PlayerLife Computer_Life = new PlayerLife();

    //Human_Play, Computer_Play is different because Card.comesIntoPlay() is called when a card is added by PlayerZone.add(Card)
    public PlayerZone Human_Play      = new PlayerZone_ComesIntoPlay(Constant.Zone.Play, Constant.Player.Human);
    public PlayerZone Human_Hand      = new DefaultPlayerZone(Constant.Zone.Hand      , Constant.Player.Human);
    public PlayerZone Human_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard , Constant.Player.Human);
    public PlayerZone Human_Library   = new DefaultPlayerZone(Constant.Zone.Library   , Constant.Player.Human);
    public PlayerZone Human_Removed   = new DefaultPlayerZone(Constant.Zone.Removed_From_Play, Constant.Player.Human);

    public PlayerZone Computer_Play      = new PlayerZone_ComesIntoPlay(Constant.Zone.Play      , Constant.Player.Computer);
    public PlayerZone Computer_Hand      = new DefaultPlayerZone(Constant.Zone.Hand      , Constant.Player.Computer);
    public PlayerZone Computer_Graveyard = new DefaultPlayerZone(Constant.Zone.Graveyard , Constant.Player.Computer);
    public PlayerZone Computer_Library   = new DefaultPlayerZone(Constant.Zone.Library   , Constant.Player.Computer);
    public PlayerZone Computer_Removed   = new DefaultPlayerZone(Constant.Zone.Removed_From_Play, Constant.Player.Computer);
}