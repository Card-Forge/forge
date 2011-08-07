package forge.card.mana;

import forge.Constant;
import forge.gui.input.Input_PayManaCostUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * <p>ManaCost class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class ManaCost {
    //holds Mana_Part objects
    //ManaPartColor is stored before ManaPartColorless
    private ArrayList<Object> manaPart;
    private HashMap<String, Integer> sunburstMap = new HashMap<String, Integer>();
    private int xcounter = 0;

//manaCost can be like "0", "3", "G", "GW", "10", "3 GW", "10 GW"
    //or "split hybrid mana" like "2/G 2/G", "2/B 2/B 2/B"
    //"GW" can be paid with either G or W

    /**
     * <p>Constructor for ManaCost.</p>
     *
     * @param manaCost a {@link java.lang.String} object.
     */
    public ManaCost(String manaCost) {
        if (manaCost.equals(""))
            manaCost = "0";

        while (manaCost.contains("X")) {
            if (manaCost.length() < 2)
                manaCost = "0";
            else
                manaCost = manaCost.replaceFirst("X ", "");
            setXcounter(getXcounter() + 1);
        }
        manaPart = split(manaCost);
    }

    /**
     * <p>getSunburst.</p>
     *
     * @return a int.
     */
    public int getSunburst() {
        int ret = sunburstMap.size();
        sunburstMap.clear();
        return ret;
    }
    
    /**
     * <p>getColorsPaid.</p>
     *
     * @return a String.
     */
    public String getColorsPaid() {
    	String s = "";
    	for (String key: sunburstMap.keySet()) {
    	    if(key.equals("black")) s+= "B";
    	    if(key.equals("blue")) s+= "U";
    	    if(key.equals("green")) s+= "G";
    	    if(key.equals("red")) s+= "R";
    	    if(key.equals("white")) s+= "W";
    	}
        return s;
    }

    /**
     * <p>getUnpaidPhyrexianMana.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<Mana_PartPhyrexian> getUnpaidPhyrexianMana() {
        ArrayList<Mana_PartPhyrexian> res = new ArrayList<Mana_PartPhyrexian>();
        for (Object o : manaPart) {
            if (o instanceof Mana_PartPhyrexian) {
                Mana_PartPhyrexian phy = (Mana_PartPhyrexian) o;

                if (!phy.isPaid())
                    res.add(phy);
            }
        }

        return res;
    }

    /**
     * <p>containsPhyrexianMana.</p>
     *
     * @return a boolean.
     */
    public boolean containsPhyrexianMana() {
        for (Object o : manaPart) {
            if (o instanceof Mana_PartPhyrexian) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>payPhyrexian.</p>
     *
     * @return a boolean.
     */
    public boolean payPhyrexian() {
        ArrayList<Mana_PartPhyrexian> Phy = getUnpaidPhyrexianMana();

        if (Phy.size() > 0) {
            Phy.get(0).payLife();

            return true;
        }

        return false;
    }

    // takes a Short Color and returns true if it exists in the mana cost. Easier for split costs
    /**
     * <p>isColor.</p>
     *
     * @param color a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isColor(String color) {
        for (Object s : manaPart) {
            if (s.toString().contains(color))
                return true;
        }
        return false;
    }

    // isNeeded(String) still used by the Computer, might have problems activating Snow abilities
    /**
     * <p>isNeeded.</p>
     *
     * @param mana a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isNeeded(String mana) {
        if (mana.length() > 1)
            mana = Input_PayManaCostUtil.getShortColorString(mana);
        Mana_Part m;
        for (int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if (m.isNeeded(mana)) return true;
        }
        return false;
    }

    /**
     * <p>isNeeded.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public boolean isNeeded(Mana mana) {
        Mana_Part m;
        for (int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if (m.isNeeded(mana)) return true;
            if (m instanceof Mana_PartSnow && mana.isSnow()) return true;
        }
        return false;
    }

    /**
     * <p>isPaid.</p>
     *
     * @return a boolean.
     */
    public boolean isPaid() {
        Mana_Part m;
        for (int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if (!m.isPaid()) return false;
        }
        return true;
    }//isPaid()

    /**
     * <p>payMana.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public boolean payMana(Mana mana) {
        return addMana(mana);
    }

    /**
     * <p>payMana.</p>
     *
     * @param color a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean payMana(String color) {
        color = Input_PayManaCostUtil.getShortColorString(color);
        return addMana(color);
    }

    /**
     * <p>increaseColorlessMana.</p>
     *
     * @param manaToAdd a int.
     */
    public void increaseColorlessMana(int manaToAdd) {
        if (manaToAdd <= 0)
            return;

        Mana_Part m;
        for (int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if (m instanceof Mana_PartColorless) {
                ((Mana_PartColorless) m).addToManaNeeded(manaToAdd);
                return;
            }
        }
        manaPart.add(new Mana_PartColorless(manaToAdd));
    }

    /**
     * <p>addMana.</p>
     *
     * @param mana a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean addMana(String mana) {
        if (!isNeeded(mana)) throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);

        Mana_Part choice = null;

        for (int i = 0; i < manaPart.size(); i++) {
            Mana_Part m = (Mana_Part) manaPart.get(i);
            if (m.isNeeded(mana)) {
                // if m is a better to pay than choice
                if (choice == null) {
                    choice = m;
                    continue;
                }
                if (m.isColor(mana) && choice.isEasierToPay(m)) {
                    choice = m;
                }
            }
        }//for
        if (choice == null)
            return false;

        choice.reduce(mana);
        if (!mana.equals(Constant.Color.Colorless)) {
            if (sunburstMap.containsKey(mana))
                sunburstMap.put(mana, sunburstMap.get(mana) + 1);
            else
                sunburstMap.put(mana, 1);
        }
        return true;
    }

    /**
     * <p>addMana.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public boolean addMana(Mana mana) {
        if (!isNeeded(mana)) throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);

        Mana_Part choice = null;

        for (int i = 0; i < manaPart.size(); i++) {
            Mana_Part m = (Mana_Part) manaPart.get(i);
            if (m.isNeeded(mana)) {
                // if m is a better to pay than choice
                if (choice == null) {
                    choice = m;
                    continue;
                }
                if (m.isColor(mana) && choice.isEasierToPay(m)) {
                    choice = m;
                }
            }
        }//for
        if (choice == null)
            return false;

        choice.reduce(mana);
        if (!mana.isColor(Constant.Color.Colorless)) {
            if (sunburstMap.containsKey(mana.getColor()))
                sunburstMap.put(mana.getColor(), sunburstMap.get(mana.getColor()) + 1);
            else
                sunburstMap.put(mana.getColor(), 1);
        }
        return true;
    }

    /**
     * <p>combineManaCost.</p>
     *
     * @param extra a {@link java.lang.String} object.
     */
    public void combineManaCost(String extra) {
        ArrayList<Object> extraParts = split(extra);

        Mana_PartColorless part = null;
        for (int i = 0; i < manaPart.size(); i++) {
            Object o = manaPart.get(i);
            if (o instanceof Mana_PartColorless)
                part = (Mana_PartColorless) o;
        }
        if (part != null) {
            manaPart.remove(part);
        }

        while (extraParts.size() > 0) {
            Object o = extraParts.get(0);
            if (o instanceof Mana_PartColorless) {
                if (part == null)
                    part = (Mana_PartColorless) o;
                else {
                    part.addToManaNeeded(((Mana_PartColorless) o).getManaNeeded());
                }
            } else {
                manaPart.add(o);
            }
            extraParts.remove(o);
        }
        if (part != null)
            manaPart.add(part);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Object> list = new ArrayList<Object>(manaPart);
        //need to reverse everything since the colored mana is stored first
        Collections.reverse(list);

        for (int i = 0; i < getXcounter(); i++)
            sb.append(" ").append("X");

        for (int i = 0; i < list.size(); i++) {
            sb.append(" ");
            sb.append(list.get(i).toString());
        }

        return sb.toString().trim();
    }

    /**
     * <p>getConvertedManaCost.</p>
     *
     * @return a int.
     */
    public int getConvertedManaCost() {
        int cmc = 0;
        for (Object s : manaPart) {
            cmc += ((Mana_Part) s).getConvertedManaCost();
        }
        return cmc;
    }

    /**
     * Returns Mana cost, adjusted slightly to make colored mana parts more significant.
     * Should only be used for comparison purposes; using this method allows the sort:
     * 2 < X 2 < 1 U < U U == UR U < X U U < X X U U
     *
     * @return The converted cost + 0.0001* the number of colored mana in the cost + 0.00001 *
     *         the number of X's in the cost
     */
    public double getWeightedManaCost() {
        double cmc = 0;
        for (Object s : manaPart) {
            cmc += ((Mana_Part) s).getConvertedManaCost();
            if (s instanceof Mana_PartColor) {
                cmc += 0.0001;
            }
        }

        cmc += 0.00001 * getXcounter();
        return cmc;
    }

    /**
     * <p>split.</p>
     *
     * @param cost a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<Object> split(String cost) {
        ArrayList<Object> list = new ArrayList<Object>();

        //handles costs like "3", "G", "GW", "10", "S"
        if (cost.length() == 1 || cost.length() == 2) {
            if (Character.isDigit(cost.charAt(0))) list.add(new Mana_PartColorless(cost));
            else if (cost.charAt(0) == 'S') list.add(new Mana_PartSnow());
            else if (cost.charAt(0) == 'P') list.add(new Mana_PartPhyrexian(cost));
            else list.add(new Mana_PartColor(cost));
        } else//handles "3 GW", "10 GW", "1 G G", "G G", "S 1"
        {
            //all costs that have a length greater than 2 have a space
            StringTokenizer tok = new StringTokenizer(cost);

            while (tok.hasMoreTokens())
                list.add(getManaPart(tok.nextToken()));

            //ManaPartColorless needs to be added AFTER the colored mana
            //in order for isNeeded() and addMana() to work correctly
            Object o = list.get(0);
            if (o instanceof Mana_PartSnow) {
                //move snow cost to the end of the list
                list.remove(0);
                list.add(o);
            }
            o = list.get(0);

            if (o instanceof Mana_PartColorless) {
                //move colorless cost to the end of the list
                list.remove(0);
                list.add(o);
            }
        }//else

        return list;
    }//split()

    /**
     * <p>Getter for the field <code>manaPart</code>.</p>
     *
     * @param partCost a {@link java.lang.String} object.
     * @return a {@link forge.card.mana.Mana_Part} object.
     */
    private Mana_Part getManaPart(String partCost) {
        if (partCost.length() == 3) {
            return new Mana_PartSplit(partCost);
        } else if (Character.isDigit(partCost.charAt(0))) {
            return new Mana_PartColorless(partCost);
        } else if (partCost.equals("S")) {
            return new Mana_PartSnow();
        } else if (partCost.startsWith("P")) {
            return new Mana_PartPhyrexian(partCost);
        } else {
            return new Mana_PartColor(partCost);
        }
    }

    /**
     * <p>Setter for the field <code>xcounter</code>.</p>
     *
     * @param xcounter a int.
     */
    public void setXcounter(int xcounter) {
        this.xcounter = xcounter;
    }

    /**
     * <p>Getter for the field <code>xcounter</code>.</p>
     *
     * @return a int.
     */
    public int getXcounter() {
        return xcounter;
    }

    /**
     * <p>removeColorlessMana.</p>
     *
     * @since 1.0.15
     */
    public void removeColorlessMana() {

        for (int i = 0; i < manaPart.size(); i++) {
            if (manaPart.get(i) instanceof Mana_PartColorless)
                manaPart.remove(manaPart.get(i));
        }
    }
}
