package forge;
import java.util.Random;

public class MyRandom
{
  public static Random random = new Random();

  //if percent is like 50, the its like 50% of the time will be true
  public static boolean percentTrue(int percent)
  {
    return percent > random.nextInt(100);
  }
}