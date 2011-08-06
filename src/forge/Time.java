package forge;
public class Time
{
  private long startTime;
  private long stopTime;

  public Time() {start();}

  public void start()
  {
    startTime = System.currentTimeMillis();
  }

  public double stop()
  {
    stopTime = System.currentTimeMillis();
    return getTime();
  }

  public double getTime()
  {
    return (stopTime - startTime) / 1000.0;
  }
}