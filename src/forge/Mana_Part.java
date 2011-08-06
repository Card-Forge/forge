package forge;
public abstract class Mana_Part
{
  abstract public String toString();
  abstract public void reduce(String mana);
  abstract public boolean isPaid();
  abstract public boolean isNeeded(String mana);

  public static void checkSingleMana(String m)
  {
    if (m.length() != 1)
      throw new RuntimeException("Mana_Part : checkMana() error, argument mana is not of length 1, mana - " + m);

    if (!(m.equals("G") || m.equals("U") || m.equals("W") ||
          m.equals("B") || m.equals("R") || m.equals("1")))
      throw new RuntimeException("Mana_Part : checkMana() error, argument mana is invalid mana, mana - " + m);
  }
}
