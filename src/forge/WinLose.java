package forge;
public class WinLose
{
  private int win;
  private int lose;
  private boolean winRecently;

  public void reset()  {win = 0; lose = 0;}
  public void addWin() {win++;  winRecently = true;}
  public void addLose(){lose++; winRecently = false;}

  public int getWin()       {return win;}
  public int getLose()      {return lose;}
  public int countWinLose() {return win + lose;}

  public boolean didWinRecently() {return winRecently;}
}