package forge.deck;

import forge.AllZone;
import forge.Card;

/**
 * <p>DownloadDeck class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class DownloadDeck {


    /**
     * <p>foundNumberCard.</p>
     *
     * @param rStr a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String foundNumberCard(String rStr) {
        int temp;
        int i;

        for (i = 0; i < rStr.length(); i++) {
            temp = rStr.codePointAt(i);
            if (temp >= 48 && temp <= 57) {
                break;

            }

        }
        if (rStr.codePointAt(i + 1) >= 48 && rStr.codePointAt(i + 1) <= 57) {
            return rStr.substring(i, i + 2);
        } else {
            return rStr.substring(i, i + 1);
        }
    }

    /**
     * <p>foundNameCard.</p>
     *
     * @param rStr a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String foundNameCard(String rStr) {
        int temp;
        int i;

        for (i = 0; i < rStr.length(); i++) {
            temp = rStr.codePointAt(i);
            if (temp >= 48 && temp <= 57) {
                break;

            }

        }
        return rStr.substring(0, i - 1);
    }


    /**
     * <p>removeSpace.</p>
     *
     * @param rStr a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeSpace(String rStr) {
        int temp;
        int i;

        for (i = 0; i < rStr.length(); i++) {
            temp = rStr.codePointAt(i);
            if (temp != 32) {
                break;

            }

        }
        return rStr.substring(i);
    }

    /**
     * <p>removeSpaceBack.</p>
     *
     * @param rStr a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeSpaceBack(String rStr) {
        int temp;
        int i;

        for (i = rStr.length() - 1; i > -1; i = i - 1) {
            temp = rStr.codePointAt(i);
            if (temp != 32) {
                break;

            }

        }
        return rStr.substring(0, i + 1);
    }

    /**
     * <p>removeFoundNumberCard.</p>
     *
     * @param rStr a {@link java.lang.String} object.
     * @param Number a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeFoundNumberCard(String rStr, String Number) {
        int a;
        int temp;
        a = rStr.indexOf(Number);
        temp = rStr.codePointAt(a + 1);
        if (temp >= 48 && temp <= 57) {
            return rStr.substring(a + 2);
        } else {
            return rStr.substring(a + 1);
        }

    }

    /**
     * <p>removeFoundNameCard.</p>
     *
     * @param rStr a {@link java.lang.String} object.
     * @param Name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeFoundNameCard(String rStr, String Name) {
        int a;
        a = Name.length();
        return rStr.substring(a);

    }

    /**
     * <p>isCardSupport.</p>
     *
     * @param CardName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isCardSupport(String CardName) {
    	// TODO: using AllZone.getCardFactory().getCard() would probably be much faster.
    	
        for (Card gCard : AllZone.getCardFactory()) {
            if (CardName.equalsIgnoreCase(gCard.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>getCardDownload.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param CardName a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public Card getCardDownload(Card c, String CardName) {
    	// TODO: using AllZone.getCardFactory().getCard() would probably be much faster.
    	
        for (Card newCard : AllZone.getCardFactory()) {
            if (CardName.equalsIgnoreCase(newCard.getName())) {
                return newCard;
            }
        }

        return null;

    }

}

	

