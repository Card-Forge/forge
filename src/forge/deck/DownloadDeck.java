package forge.deck;

import forge.AllZone;
import forge.Card;
import forge.CardList;

public class DownloadDeck {


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
        }
        else {
            return rStr.substring(i, i + 1);
        }
    }

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

    public String removeFoundNumberCard(String rStr, String Number) {
        int a;
        int temp;
        a = rStr.indexOf(Number);
        temp = rStr.codePointAt(a + 1);
        if (temp >= 48 && temp <= 57) {
            return rStr.substring(a + 2);
        }
        else {
            return rStr.substring(a + 1);
        }

    }

    public String removeFoundNameCard(String rStr, String Name) {
        int a;
        a = Name.length();
        return rStr.substring(a);

    }

    public boolean isCardSupport(String CardName) {
        CardList all = AllZone.CardFactory.getAllCards();

        Card gCard;
        for (int i = 0; i < all.size(); i++) {
            gCard = all.getCard(i);
            if (CardName.equalsIgnoreCase(gCard.getName())) {
                return true;
            }
        }
        return false;
    }

    public Card getCardDownload(Card c, String CardName) {
        CardList all = AllZone.CardFactory.getAllCards();

        Card newCard = null;

        for (int i = 0; i < all.size(); i++) {
            newCard = all.getCard(i);

            if (CardName.equalsIgnoreCase(newCard.getName())) {
                return newCard;
            }
        }

        return newCard;

    }

}

	

