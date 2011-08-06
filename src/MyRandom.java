import java.util.*;

public class MyRandom
{
  public static final Random random = new Random();

  //if percent is like 50, the its like 50% of the time will be true
  public static boolean percentTrue(int percent)
  {
    return percent > random.nextInt(100);
  }
  public static void shuffle(Object[] o)
  {
    for(int i = 0; i < o.length; i++)
    {
      swap(o, i, random.nextInt(o.length));
      swap(o, i, random.nextInt(o.length));
    }
  }
  private static void swap(Object o[], int index_1, int index_2)
  {
      Object hold = o[index_1];
      o[index_1] = o[index_2];
      o[index_2] = hold;
  }
}