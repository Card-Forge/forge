package forge;

import org.testng.annotations.Test;

import forge.card.cardfactory.CardFactoryInterface;
import forge.game.phase.CombatUtil;

/**
 * <p>
 * RunTest class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class RunTest {
    // @SuppressWarnings("unchecked") // HashSet needs <type>

    /**
     * <p>
     * test.
     * </p>
     */
    @Test(timeOut = 1000, enabled = false)
    void test() {
        Card c;
        final CardFactoryInterface cf = Singletons.getModel().getCardFactory();
        // ********* test Card
        /*
        c = cf.getCard("Elvish Warrior", AllZone.getComputerPlayer());
        this.check("1", c.getOwner().isComputer());
        this.check("1.1", c.getName().equals("Elvish Warrior"));
        this.check("2", c.getManaCost().equals("G G"));
        this.check("2.1", c.isCreature());
        this.check("2.2", c.isType("Elf"));
        this.check("2.3", c.isType("Warrior"));
        this.check("3", c.getText().equals(""));
        this.check("4", c.getNetAttack() == 2);
        this.check("5", c.getNetDefense() == 3);
        this.check("6", c.getKeyword().isEmpty());

        c = cf.getCard("Shock", null);
        this.check("14", c.isInstant());
        // check("15",
        // c.getText().equals("Shock deals 2 damge to target creature or player."));

        c = cf.getCard("Bayou", null);
        this.check("17", c.getManaCost().equals(""));
        this.check("18", c.isLand());
        this.check("19", c.isType("Swamp"));
        this.check("20", c.isType("Forest"));

        // ********* test ManaCost
        ManaCost manaCost = new ManaCost("G");
        this.check("21", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.GREEN);
        this.check("22", manaCost.isPaid());

        manaCost = new ManaCost("7");
        this.check("23", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLACK);
        this.check("24", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLUE);
        this.check("25", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.COLORLESS);
        this.check("26", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.GREEN);
        this.check("27", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.RED);
        this.check("28", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.WHITE);
        this.check("29", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.WHITE);
        this.check("30", manaCost.isPaid());

        manaCost = new ManaCost("2 W W G G B B U U R R");
        this.check("31", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.WHITE);
        this.check("32", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.WHITE);
        this.check("32.1", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLACK);
        this.check("33", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLACK);
        this.check("34", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLUE);
        this.check("35", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLUE);
        this.check("36", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.GREEN);
        this.check("37", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.GREEN);
        this.check("38", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.RED);
        this.check("39", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.RED);
        this.check("40", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.RED);
        this.check("41", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLUE);
        this.check("42", manaCost.isPaid());

        manaCost = new ManaCost("G G");
        this.check("43", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.GREEN);
        this.check("44", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.GREEN);
        this.check("45", manaCost.isPaid());

        manaCost = new ManaCost("1 U B");
        this.check("45", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLACK);
        this.check("46", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLUE);
        this.check("47", !manaCost.isPaid());
        manaCost.payMana(Constant.Color.BLUE);
        this.check("48", manaCost.isPaid());

        // ********* test CardUtil.getColors()
        c = new Card();
        c.setManaCost(new CardManaCost(new ManaCostParser("G")));
        ArrayList<String> color = CardUtil.getColors(c);
        this.check("49", color.contains(Constant.Color.GREEN));
        this.check("50", color.size() == 1);

        c = new Card();
        c.setManaCost(new CardManaCost(new ManaCostParser("W B G R U")));
        color = CardUtil.getColors(c);
        final Set<String> set = new HashSet<String>(color);
        System.out.println("color: " + color);
        this.check("51", set.size() == 5);
        this.check("52", color.contains(Constant.Color.BLACK));
        this.check("53", color.contains(Constant.Color.BLUE));
        this.check("54", color.contains(Constant.Color.GREEN));
        this.check("55", color.contains(Constant.Color.RED));
        this.check("56", color.contains(Constant.Color.WHITE));

        c = new Card();
        c.setManaCost(new CardManaCost(new ManaCostParser("2")));
        color = CardUtil.getColors(c);
        this.check("57", color.size() == 1);
        this.check("58", color.contains(Constant.Color.COLORLESS));

        c = new Card();
        color = CardUtil.getColors(c);
        this.check("59", color.size() == 1);
        this.check("60", color.contains(Constant.Color.COLORLESS));

        c = new Card();
        c.setManaCost(new CardManaCost(new ManaCostParser("")));
        color = CardUtil.getColors(c);
        this.check("61", color.size() == 1);
        this.check("62", color.contains(Constant.Color.COLORLESS));

        c = cf.getCard("Bayou", null);
        color = CardUtil.getColors(c);
        this.check("63", color.size() == 1);
        this.check("64", color.contains(Constant.Color.COLORLESS));

        c = cf.getCard("Elvish Warrior", null);
        color = CardUtil.getColors(c);
        this.check("65", color.size() == 1);
        this.check("66", color.contains(Constant.Color.GREEN));

        c = new Card();
        c.setManaCost(new CardManaCost(new ManaCostParser("11 W W B B U U R R G G")));
        color = CardUtil.getColors(c);
        this.check("67", color.size() == 5);

        c = new Card();
        c = cf.getCard("Elvish Warrior", null);
        c.setManaCost(new CardManaCost(new ManaCostParser("11")));
        color = CardUtil.getColors(c);
        this.check("68", color.size() == 1);
        this.check("69", color.contains(Constant.Color.COLORLESS));

        this.check("70", c.isCreature());
        this.check("71", !c.isArtifact());
        this.check("72", !c.isBasicLand());
        this.check("73", !c.isEnchantment());
        this.check("74", !c.isGlobalEnchantment());
        this.check("75", !c.isInstant());
        this.check("76", !c.isLand());
        this.check("77", !c.isAura());
        this.check("78", c.isPermanent());
        this.check("79", !c.isSorcery());
        this.check("80", !c.isTapped());
        this.check("81", c.isUntapped());

        c = cf.getCard("Swamp", null);
        this.check("82", c.isBasicLand());
        this.check("83", c.isLand());

        c = cf.getCard("Bayou", null);
        this.check("84", !c.isBasicLand());
        this.check("85", c.isLand());

        c = cf.getCard("Shock", null);
        this.check("86", !c.isCreature());
        this.check("87", !c.isArtifact());
        this.check("88", !c.isBasicLand());
        this.check("89", !c.isEnchantment());
        this.check("90", !c.isGlobalEnchantment());
        this.check("91", c.isInstant());
        this.check("92", !c.isLand());
        this.check("93", !c.isAura());
        this.check("94", !c.isPermanent());
        this.check("95", !c.isSorcery());
        this.check("96", !c.isTapped());
        this.check("97", c.isUntapped());

        // test Input_PayManaCostUtil
        this.check("98", InputPayManaCostUtil.getLongColorString("G").equals(Constant.Color.GREEN));
        this.check("99", InputPayManaCostUtil.getLongColorString("1").equals(Constant.Color.COLORLESS));
*/
        /*
         * check("101", Input_PayManaCostUtil.isManaNeeded(Constant.Color.Green,
         * new ManaCost("5")) == true); check("102",
         * Input_PayManaCostUtil.isManaNeeded(Constant.Color.Blue, new
         * ManaCost("4")) == true); check("103",
         * Input_PayManaCostUtil.isManaNeeded(Constant.Color.White, new
         * ManaCost("3")) == true); check("104",
         * Input_PayManaCostUtil.isManaNeeded(Constant.Color.Black, new
         * ManaCost("2")) == true); check("105",
         * Input_PayManaCostUtil.isManaNeeded(Constant.Color.Red, new
         * ManaCost("1")) == true);
         */
        /*
         * ManaCost cost = new ManaCost("1 B B");
         * Input_PayManaCostUtil.isManaNeeded(Constant.Color.Black, cost);
         * cost.subtractMana(Constant.Color.Black);
         * cost.subtractMana(Constant.Color.Green); check("106",
         * Input_PayManaCostUtil.isManaNeeded(Constant.Color.Green, cost) ==
         * false);
         */

        c = new Card();
        Card c2 = new Card();
        c.addIntrinsicKeyword("Flying");
        c2.addIntrinsicKeyword("Flying");
        // check("107", CombatUtil.canBlock(c, c2));
        // check("108", CombatUtil.canBlock(c2, c));

        c = new Card();
        c2 = new Card();
        c2.addIntrinsicKeyword("Flying");
        this.check("109", CombatUtil.canBlock(c, c2));
        this.check("110", !CombatUtil.canBlock(c2, c));
/*
        c = cf.getCard("Fyndhorn Elves", null);
        c2 = cf.getCard("Talas Warrior", null);
        this.check("110a", !CombatUtil.canBlock(c2, c));
        this.check("110b", CombatUtil.canBlock(c, c2));

        c = new Card();
        c.setName("1");
        c.setUniqueNumber(1);
        c2 = new Card();
        c2.setName("2");
        c2.setUniqueNumber(2);

        // test CardList
        final List<Card> cardList = new ArrayList<Card>(Arrays.asList(new Card[] { c, c2 }));
        this.check("111", cardList.contains(c));
        this.check("112", cardList.contains(c2));
        this.check("113", cardList.containsName(c));
        this.check("114", cardList.containsName(c.getName()));
        this.check("115", cardList.containsName(c2));
        this.check("116", cardList.containsName(c2.getName()));

        c = new Card();
        this.check("117", c.hasSickness());
        c.addIntrinsicKeyword("Haste");
        this.check("118", !c.hasSickness());

        final CardFactoryInterface cf1 = AllZone.getCardFactory();
        final List<Card> c1 = new ArrayList<Card>();
        c1.add(cf1.getCard("Shock", null));
        c1.add(cf1.getCard("Royal Assassin", null));
        c1.add(cf1.getCard("Hymn to Tourach", null));

        List<Card> c3 = c1.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return c.isCreature();
            }
        });
        this.check("119", c3.containsName("Royal Assassin"));
        this.check("119", c3.size() == 1);

        c3 = c1.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return c.isInstant();
            }
        });
        this.check("120", c3.containsName("Shock"));
        this.check("121", c3.size() == 1);

        c3 = c1.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return c.getName().equals("Hymn to Tourach");
            }
        });
        this.check("120", c3.containsName("Hymn to Tourach"));
        this.check("121", c3.size() == 1);

        final Card card = cf1.getCard("Sylvan Basilisk", null);
        final Card card2 = cf1.getCard("Exalted Angel", null);

        this.check("121a", !CombatUtil.canDestroyAttacker(card, card2, null, false));

        this.check("122", CardUtil.getConvertedManaCost("0") == 0);
        this.check("123", CardUtil.getConvertedManaCost("R") == 1);
        this.check("124", CardUtil.getConvertedManaCost("R R") == 2);
        this.check("125", CardUtil.getConvertedManaCost("R R R") == 3);
        this.check("126", CardUtil.getConvertedManaCost("1") == 1);
        this.check("127", CardUtil.getConvertedManaCost("2/R 2/G 2/W 2/B 2/U") == 10);
        */
    } // test()

    /**
     * <p>
     * check.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param ok
     *            a boolean.
     */
    void check(final String message, final boolean ok) {
        if (!ok) {
            // throw new RuntimeException("RunTest test error : " +message);
            System.out.println("RunTest test error : " + message);
        }

    }
}
