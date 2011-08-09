package forge;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 * Date: 6/24/11
 * Time: 8:42 PM
 * To change this template use File | Settings | File Templates.
 */
class DeckColors {
    public String Color1 = "none";
    public String Color2 = "none";
    //public String Splash = "none";
    public String Mana1 = "";
    public String Mana2 = "";
    //public String ManaS = "";

    /**
     * <p>Constructor for deckColors.</p>
     *
     * @param c1 a {@link java.lang.String} object.
     * @param c2 a {@link java.lang.String} object.
     * @param sp a {@link java.lang.String} object.
     */
    public DeckColors(String c1, String c2, String sp) {
        Color1 = c1;
        Color2 = c2;
        //Splash = sp;
    }

    /**
     * <p>Constructor for DeckColors.</p>
     */
    public DeckColors() {

    }

    /**
     * <p>ColorToMana.</p>
     *
     * @param color a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String ColorToMana(String color) {
        String Mana[] = {"W", "U", "B", "R", "G"};

        for (int i = 0; i < Constant.Color.onlyColors.length; i++) {
            if (Constant.Color.onlyColors[i].equals(color))
                return Mana[i];
        }

        return "";
    }


}
