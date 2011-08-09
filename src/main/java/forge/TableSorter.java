package forge;


import forge.properties.NewConstants;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * <p>TableSorter class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("unchecked") // Comparable needs <type>
public class TableSorter implements Comparator<Card>, NewConstants {
    private final int column;
    private boolean ascending;
    private boolean col7mod;

    private CardList all;

    //used by compare()
    @SuppressWarnings("rawtypes")
    private Comparable aCom = null;
    @SuppressWarnings("rawtypes")
    private Comparable bCom = null;

    //used if in_column is 7, new cards first - the order is based on cards.txt
    //static because this should only be read once
    //static to try to reduce file io operations
    //private static HashMap<String, Integer> cardsTxt = null;

    //                             0       1       2       3        4     5          6          7
    //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"}; New cards first - the order is based on cards.txt

    /**
     * <p>Constructor for TableSorter.</p>
     *
     * @param in_all a {@link forge.CardList} object.
     * @param in_column a int.
     * @param in_ascending a boolean.
     */
    public TableSorter(CardList in_all, int in_column, boolean in_ascending) {
        all = new CardList(in_all.toArray());
        column = in_column;
        ascending = in_ascending;
    }

    /**
     * <p>Constructor for TableSorter.</p>
     *
     * @param in_all a {@link forge.CardList} object.
     * @param in_column a int.
     * @param in_ascending a boolean.
     * @param in_col7mod a boolean.
     */
    public TableSorter(CardList in_all, int in_column, boolean in_ascending, boolean in_col7mod) {
        all = new CardList(in_all.toArray());
        column = in_column;
        ascending = in_ascending;
        col7mod = in_col7mod;
    }

    /**
     * <p>compare.</p>
     *
     * @param a a {@link forge.Card} object.
     * @param b a {@link forge.Card} object.
     * @return a int.
     */
    final public int compare(Card a, Card b) {

        if (column == 0)//Qty
        {
            aCom = Integer.valueOf(countCardName(a.getName(), all));
            bCom = Integer.valueOf(countCardName(b.getName(), all));
        } else if (column == 1)//Name
        {
            aCom = a.getName();
            bCom = b.getName();
        } else if (column == 2)//Cost
        {
            aCom = Double.valueOf(CardUtil.getWeightedManaCost(a.getManaCost()));
            bCom = Double.valueOf(CardUtil.getWeightedManaCost(b.getManaCost()));

            if (a.isLand())
                aCom = Double.valueOf(-1);
            if (b.isLand())
                bCom = Double.valueOf(-1);
        } else if (column == 3)//Color
        {
            aCom = getColor(a);
            bCom = getColor(b);
        } else if (column == 4)//Type
        {
            aCom = getType(a);
            bCom = getType(b);
        } else if (column == 5)//Stats, attack and defense
        {
            if (a.isCreature()) {
                aCom = a.getBaseAttackString() + "." + a.getBaseDefenseString();
            } else {
                aCom = "";
            }

            if (b.isCreature()) {
                bCom = b.getBaseAttackString() + "." + b.getBaseDefenseString();
            } else {
                bCom = "";
            }
        } else if (column == 6)//Rarity
        {
            aCom = getRarity(a);
            bCom = getRarity(b);
        } else if (column == 7 && col7mod == false)//Value
        {
            aCom = getValue(a);
            bCom = getValue(b);
        } else if (column == 7 && col7mod == true)//Set
        {
            aCom = SetInfoUtil.getSetIndex(a.getCurSetCode());
            bCom = SetInfoUtil.getSetIndex(b.getCurSetCode());
        } else if (column == 8)//AI
        {
            aCom = getAI(a);
            bCom = getAI(b);
        }
        /*else if (column == 99)//New First
          {
              aCom = sortNewFirst(a);
              bCom = sortNewFirst(b);
          }*/

        if (ascending)
            return aCom.compareTo(bCom);
        else
            return bCom.compareTo(aCom);
    }//compare()

    /**
     * <p>countCardName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param c a {@link forge.CardList} object.
     * @return a int.
     */
    final private int countCardName(String name, CardList c) {
        int count = 0;
        for (int i = 0; i < c.size(); i++)
            if (name.equals(c.get(i).getName()))
                count++;

        return count;
    }

    /**
     * <p>getRarity.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.lang.Integer} object.
     */
    final private Integer getRarity(Card c) {
        String rarity = c.getRarity();

        if (rarity.equals("new"))
            return 1;

        if (!c.getCurSetCode().equals("")) {
            SetInfo si = SetInfoUtil.getSetInfo_Code(c.getSets(), c.getCurSetCode());
            if (si != null)
                rarity = si.Rarity;
        }

        if (rarity.equals("Common"))
            return 2;
        else if (rarity.equals("Uncommon"))
            return 3;
        else if (rarity.equals("Rare"))
            return 4;
        else if (rarity.equals("Mythic"))
            return 5;
        else if (rarity.equals("Special"))
            return 6;
        else if (rarity.equals("Land"))
            return 7;
        else
            return 8;

        // This older form of the method no longer works as it is not compatible with set info.
        /*
        if(c.getRarity().equals("Common"))
            return Integer.valueOf(1);
        else if(c.getRarity().equals("Uncommon"))
            return Integer.valueOf(2);
        else if(c.getRarity().equals("Rare"))
            return Integer.valueOf(3);
        else if(c.getRarity().equals("Land"))
            return Integer.valueOf(4);
        else
            return Integer.valueOf(5);
        */
    }

    /**
     * <p>getValue.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.lang.Long} object.
     */
    final private Long getValue(Card c) {
        return c.getValue();
    }

    /**
     * <p>getColor.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    final public static String getColor(Card c) {
        ArrayList<String> list = CardUtil.getColors(c);

        if (list.size() == 1)
            return list.get(0).toString();

        return "multi";
    }

    /**
     * <p>getAI.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.lang.Integer} object.
     */
    final private Integer getAI(Card c) {
        if (c.getSVar("RemAIDeck").equals("True")
                && c.getSVar("RemRandomDeck").equals("True"))
            return Integer.valueOf(3);
        else if (c.getSVar("RemAIDeck").equals("True"))
            return Integer.valueOf(4);
        else if (c.getSVar("RemRandomDeck").equals("True"))
            return Integer.valueOf(2);
        else
            return Integer.valueOf(1);
    }

    /**
     * <p>getType.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.lang.Comparable} object.
     */
    final private Comparable<String> getType(Card c) {
        return c.getType().toString();
    }

}
