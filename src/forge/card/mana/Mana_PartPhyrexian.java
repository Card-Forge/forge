package forge.card.mana;

public class Mana_PartPhyrexian extends Mana_Part {
    private Mana_PartColor wrappedColor;
    private String color;

    public Mana_PartPhyrexian(String manaCostToPay)
    {
        wrappedColor = new Mana_PartColor(manaCostToPay.substring(1));
        color = manaCostToPay.substring(1);
    }

    public boolean isEasierToPay(Mana_Part part)
    {
        return true;
    }

    public String toString()
    {
        return wrappedColor.toString().equals("") ? "" : "P" + wrappedColor.toString();
    }

    public boolean isPaid()
    {
        return wrappedColor.isPaid();
    }

    public boolean isColor(String mana)
    {
        return wrappedColor.isColor(mana);
    }

    public boolean isColor(Mana mana)
    {
        return wrappedColor.isColor(mana);
    }

    public boolean isNeeded(String mana)
    {
        return wrappedColor.isNeeded(mana);
    }

    public boolean isNeeded(Mana mana)
    {
        return wrappedColor.isNeeded(mana);
    }

    public void reduce(String mana)
    {
        wrappedColor.reduce(mana);
    }

    public void reduce(Mana mana)
    {
        wrappedColor.reduce(mana);
    }

    public int getConvertedManaCost()
    {
        return wrappedColor.getConvertedManaCost();
    }

    public void payLife()
    {
        wrappedColor.reduce(color);
    }
}
