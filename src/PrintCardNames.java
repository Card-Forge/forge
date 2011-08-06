import java.util.*;

import java.io.*;

public class PrintCardNames
{
  public static void main(String[] args) throws IOException
  {
    FileWriter out = new FileWriter("c:\\all-card-names.txt");
    ArrayList<String> list = getCardNames();

    for(int i = 0; i < list.size(); i++)
      out.write(list.get(i) +"\r\n");

    out.flush();
    out.close();
  }

  static ArrayList<String> getCardNames()
  {
    CardList c = AllZone.CardFactory.getAllCards();
    ArrayList<String> list = new ArrayList<String>();
    for(int i = 0; i < c.size(); i++)
      list.add(c.get(i).getName());
//      list.add(c.get(i).getName() +"\t "  +GuiDisplayUtil.cleanString(c.get(i).getName()) +".jpg");

    Collections.sort(list);
    return list;
  }
}
