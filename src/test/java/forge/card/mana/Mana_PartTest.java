package forge.card.mana;

import forge.Card;
import forge.Constant;
import org.testng.annotations.Test;

/**
 * <p>Mana_PartTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class Mana_PartTest {
    /**
     * <p>testPayManaCost.</p>
     */
    @Test(groups = {"UnitTest", "fast"}, timeOut = 5000)
    public void testPayManaCost() {
        {
            //test constructor
            ManaCost p = new ManaCost("G");
            p = new ManaCost("U");
            p = new ManaCost("W");
            p = new ManaCost("R");
            p = new ManaCost("B");
            p = new ManaCost("0");
            p = new ManaCost("1");
            p = new ManaCost("2");
            p = new ManaCost("3");
            p = new ManaCost("4");
            p = new ManaCost("5");
            p = new ManaCost("6");
            p = new ManaCost("7");
            p = new ManaCost("8");
            p = new ManaCost("9");
            p = new ManaCost("10");

            p = new ManaCost("GW");
            p = new ManaCost("1 G");
            p = new ManaCost("1 GW");
            p = new ManaCost("GW GW");
            p = new ManaCost("GW GW GW");
            p = new ManaCost("GW GW GW GW");

            p = new ManaCost("G G");
            p = new ManaCost("G G G");
            p = new ManaCost("G G G");
            p = new ManaCost("G G G G");

            p = new ManaCost("2 GW GW GW");
            p = new ManaCost("3 G G G");
            p = new ManaCost("12 GW GW GW");
            p = new ManaCost("11 G G G");

            p = new ManaCost("2/U");
            p = new ManaCost("2/B 2/B");
            p = new ManaCost("2/G 2/G 2/G");
            p = new ManaCost("2/R 2/R 2/R 2/R");
            p = new ManaCost("2/W 2/B 2/U 2/R 2/G");
        }

        {
            ManaCost p = new ManaCost("2/U");

            check(0.3, p.isNeeded("G"));
            check(0.4, p.isNeeded("U"));
            check(0.5, p.isNeeded("B"));
            check(0.6, p.isNeeded("W"));
            check(0.7, p.isNeeded("R"));
            check(0.8, p.isNeeded("1"));

            p.addMana("U");
            check(0.9, p.isPaid());

            check(0.91, !p.isNeeded("R"));
        }


        {
            ManaCost p = new ManaCost("G");
            check(1, p.isNeeded("G"));

            check(1.1, !p.isNeeded("U"));
            check(1.2, !p.isNeeded("B"));
            check(1.3, !p.isNeeded("W"));
            check(1.4, !p.isNeeded("R"));
            check(1.5, !p.isNeeded("1"));

            p.addMana("G");
            check(2, p.isPaid());

            check(2.1, !p.isNeeded("G"));

        }

        {
            ManaCost p = new ManaCost("1");

            check(3, p.isNeeded("G"));
            check(4, p.isNeeded("U"));
            check(5, p.isNeeded("B"));
            check(6, p.isNeeded("W"));
            check(7, p.isNeeded("R"));
            check(8, p.isNeeded("1"));

            p.addMana("B");
            check(9, p.isPaid());

            check(9.1, !p.isNeeded("R"));
        }

        {
            ManaCost p = new ManaCost("GW");

            check(10, p.isNeeded("G"));
            check(13, p.isNeeded("W"));

            check(11, !p.isNeeded("U"));
            check(12, !p.isNeeded("B"));
            check(14, !p.isNeeded("R"));
            check(15, !p.isNeeded("1"));

            p.addMana("W");
            check(16, p.isPaid());

            check(17, !p.isNeeded("W"));
        }


        {
            ManaCost p = new ManaCost("BR");

            check(17.1, p.isNeeded("B"));
            check(17.2, p.isNeeded("R"));

            check(17.3, !p.isNeeded("U"));
            check(17.4, !p.isNeeded("W"));
            check(17.5, !p.isNeeded("G"));
            check(17.6, !p.isNeeded("1"));

            p.addMana("R");
            check(17.7, p.isPaid());

            check(17.8, !p.isNeeded("R"));
        }


        {
            ManaCost p = new ManaCost("1 G G");

            p.addMana("G");

            check(18.1, p.isNeeded("G"));
            check(18.2, p.isNeeded("W"));
            check(18.3, p.isNeeded("U"));
            check(18.4, p.isNeeded("B"));
            check(18.5, p.isNeeded("R"));
            check(18.6, p.isNeeded("1"));

            p.addMana("1");
            p.addMana("G");

            check(18.7, p.isPaid());

            check(18.8, !p.isNeeded("W"));
        }

        {
            ManaCost p = new ManaCost("0");

            check(19.1, !p.isNeeded("1"));
            check(19.2, !p.isNeeded("G"));
            check(19.3, !p.isNeeded("U"));

            check(19.4, p.isPaid());

            check(19.5, !p.isNeeded("R"));
        }

        {
            ManaCost p = new ManaCost("G G");

            check(20.1, !p.isNeeded("1"));
            check(20.2, p.isNeeded("G"));

            check(20.3, !p.isNeeded("U"));

            p.addMana("G");
            p.addMana("G");

            check(20.4, p.isPaid());

            check(20.5, !p.isNeeded("B"));
        }

        {
            ManaCost p = new ManaCost("G G G");

            check(21.1, !p.isNeeded("W"));
            check(21.2, p.isNeeded("G"));

            check(21.3, !p.isNeeded("R"));

            p.addMana("G");
            p.addMana("G");
            p.addMana("G");

            check(21.4, p.isPaid());

            check(21.5, !p.isNeeded("U"));
        }

        {
            ManaCost p = new ManaCost("G G G G");

            check(22.1, !p.isNeeded("W"));
            check(22.2, p.isNeeded("G"));

            check(22.3, !p.isNeeded("R"));

            p.addMana("G");
            p.addMana("G");
            p.addMana("G");
            p.addMana("G");

            check(22.4, p.isPaid());

            check(22.5, !p.isNeeded("G"));
        }

        {
            ManaCost p = new ManaCost("GW");

            check(23.1, p.isNeeded("W"));
            check(23.2, p.isNeeded("G"));
            check(23.3, !p.isNeeded("R"));

            p.addMana("G");

            check(23.4, p.isPaid());

            check(23.5, !p.isNeeded("G"));
        }

        {
            ManaCost p = new ManaCost("GW");

            check(24.1, p.isNeeded("W"));
            check(24.2, p.isNeeded("G"));
            check(24.3, !p.isNeeded("U"));

            p.addMana("W");

            check(24.4, p.isPaid());

            check(24.5, !p.isNeeded("W"));
        }

        {
            ManaCost p = new ManaCost("3 GW GW");

            check(25.1, p.isNeeded("W"));
            check(25.2, p.isNeeded("G"));
            check(25.3, p.isNeeded("U"));

            p.addMana("1");
            p.addMana("1");
            p.addMana("1");

            check(25.4, p.isNeeded("W"));
            check(25.5, p.isNeeded("G"));
            check(25.6, !p.isNeeded("U"));

            p.addMana("G");
            p.addMana("W");

            check(25.7, p.isPaid());

            check(25.8, !p.isNeeded("W"));
            check(25.9, !p.isNeeded("G"));
            check(25.10, !p.isNeeded("1"));
            check(25.11, !p.isNeeded("R"));
        }

        {
            ManaCost p = new ManaCost("4");

            check(26.1, p.isNeeded("W"));
            check(26.2, p.isNeeded("G"));
            check(26.3, p.isNeeded("U"));

            p.addMana("1");
            p.addMana("1");
            p.addMana("1");
            p.addMana("1");

            check(26.4, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("10");

            p.addMana("G");
            p.addMana("W");
            p.addMana("R");
            p.addMana("U");
            p.addMana("B");

            p.addMana("1");

            p.addMana("W");
            p.addMana("R");
            p.addMana("U");
            p.addMana("B");

            check(27, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("12 G GW");

            for (int i = 0; i < 12; i++)
                p.addMana("R");

            p.addMana("G");
            p.addMana("W");

            check(28, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("2 W B U R G");

            for (int i = 0; i < 1; i++)
                p.addMana("R");

            for (int i = 0; i < 2; i++)
                p.addMana("1");

            for (int i = 0; i < 1; i++) {
                p.addMana("G");
                p.addMana("W");
                p.addMana("B");
                p.addMana("U");

            }
            check(29, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("W B U R G W");

            p.addMana("R");
            p.addMana("G");
            p.addMana("B");
            p.addMana("U");

            p.addMana("W");
            p.addMana("W");

            check(30, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("W B U R G W B U R G");

            for (int i = 0; i < 2; i++) {
                p.addMana("W");
                p.addMana("R");
                p.addMana("G");
                p.addMana("B");
                p.addMana("U");
            }

            check(31, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("2 W B U R G W B U R G G");

            for (int i = 0; i < 2; i++) {
                p.addMana("W");
                p.addMana("R");
                p.addMana("G");
                p.addMana("B");
                p.addMana("U");
            }

            p.addMana("1");
            p.addMana("1");
            p.addMana("G");

            check(32, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("1 B R");

            p.addMana("B");
            p.addMana("1");
            p.addMana("R");

            check(33, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("B R");

            p.addMana("B");
            p.addMana("R");

            check(34, p.isPaid());
        }


        {
            ManaCost p = new ManaCost("2/B 2/B 2/B");

            check(35, p.isNeeded("G"));

            p.addMana("B");
            check(36, p.toString().equals("2/B 2/B"));

            p.addMana("B");
            check(37, p.toString().equals("2/B"));

            p.addMana("B");
            check(38, p.isPaid());
        }


        {
            ManaCost p = new ManaCost("2/G");

            p.addMana("1");
            check(39, p.toString().equals("1"));

            p.addMana("W");
            check(40, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("2/R 2/R");

            p.addMana("1");
            check(41, p.toString().equals("2/R 1"));

            p.addMana("W");
            check(42, p.toString().equals("2/R"));
        }

        {
            ManaCost p = new ManaCost("2/W 2/W");

            for (int i = 0; i < 4; i++) {
                check(43, p.isPaid() == false);
                p.addMana("1");
            }

            check(44, p.isPaid());
        }

        {
            ManaCost p = new ManaCost("2/W 2/B 2/U 2/R 2/G");
            check(45, p.isPaid() == false);

            p.addMana("B");
            p.addMana("R");
            p.addMana("G");
            p.addMana("W");
            p.addMana("U");

            check(45.1, p.isPaid(), p);
        }

        {
            ManaCost p = new ManaCost("2/W 2/B 2/U 2/R 2/G");
            check(46, p.isPaid() == false);

            Card c = new Card();

            p.addMana(new Mana(Constant.Color.Black, 1, c));
            p.addMana(new Mana(Constant.Color.Red, 1, c));
            p.addMana(new Mana(Constant.Color.Green, 1, c));
            p.addMana(new Mana(Constant.Color.White, 1, c));
            p.addMana(new Mana(Constant.Color.Blue, 1, c));

            check(46.1, p.isPaid(), p);
        }

    } //testPayManaCost()


    /**
     * <p>check.</p>
     *
     * @param n a double.
     * @param b a boolean.
     */
    void check(double n, boolean b) {
        if (!b) {
            System.out.println("failed : " + n);
        }
    }

    /**
     * <p>check.</p>
     *
     * @param n a double.
     * @param b a boolean.
     * @param p a {@link forge.card.mana.ManaCost} object.
     */
    void check(double n, boolean b, ManaCost p) {
        if (!b) {
            System.out.println("failed : " + n);
            System.out.println(p.toString());
        }
    }
}
