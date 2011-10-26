package forge;

import java.util.ArrayList;
import java.util.Comparator;

import com.esotericsoftware.minlog.Log;

import forge.card.cardFactory.CardFactoryUtil;

/**
 * <p>
 * CardListUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardListUtil {
    /**
     * <p>
     * filterToughness.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @param atLeastToughness
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList filterToughness(final CardList in, final int atLeastToughness) {
        CardList out = new CardList();
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i).getNetDefense() <= atLeastToughness) {
                out.add(in.get(i));
            }
        }

        return out;
    }

    // the higher the defense the better

    /**
     * <p>
     * sortDefense.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortDefense(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                return b.getNetDefense() - a.getNetDefense();
            }
        };
        list.sort(com);
    } // sortDefense()

    // the higher the attack the better
    /**
     * <p>
     * sortAttack.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortAttack(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {

                if (CombatUtil.isDoranInPlay()) {
                    return b.getNetDefense() - a.getNetDefense();
                } else {
                    return b.getNetAttack() - a.getNetAttack();
                }
            }
        };
        list.sort(com);
    } // sortAttack()

    // sort by "best" using the EvaluateCreature function
    // the best creatures will be first in the list
    /**
     * <p>
     * sortByEvaluateCreature.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByEvaluateCreature(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                return CardFactoryUtil.evaluateCreature(b) - CardFactoryUtil.evaluateCreature(a);
            }
        };
        list.sort(com);
    } // sortByEvaluateCreature()

    // sort by "best" using the EvaluateCreature function
    // the best creatures will be first in the list
    /**
     * <p>
     * sortByMostExpensive.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByMostExpensive(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                return b.getCMC() - a.getCMC();
            }
        };
        list.sort(com);
    } // sortByEvaluateCreature()

    // the lower the attack the better
    /**
     * <p>
     * sortAttackLowFirst.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortAttackLowFirst(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                if (CombatUtil.isDoranInPlay()) {
                    return a.getNetDefense() - b.getNetDefense();
                } else {
                    return a.getNetAttack() - b.getNetAttack();
                }
            }
        };
        list.sort(com);
    } // sortAttackLowFirst()

    /**
     * <p>
     * sortNonFlyingFirst.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortNonFlyingFirst(final CardList list) {
        sortFlying(list);
        list.reverse();
    } // sortNonFlyingFirst

    // the creature with flying are better
    /**
     * <p>
     * sortFlying.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortFlying(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                if (a.hasKeyword("Flying") && b.hasKeyword("Flying")) {
                    return 0;
                } else if (a.hasKeyword("Flying")) {
                    return -1;
                } else if (b.hasKeyword("Flying")) {
                    return 1;
                }

                return 0;
            }
        };
        list.sort(com);
    } // sortFlying()

    // sort by keyword
    /**
     * <p>
     * sortByKeyword.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param kw
     *            a {@link java.lang.String} object.
     */
    public static void sortByKeyword(final CardList list, final String kw) {
        final String keyword = kw;
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                if (a.hasKeyword(keyword) && b.hasKeyword(keyword)) {
                    return 0;
                } else if (a.hasKeyword(keyword)) {
                    return -1;
                } else if (b.hasKeyword(keyword)) {
                    return 1;
                }

                return 0;
            }
        };
        list.sort(com);
    } // sortByKeyword()

    /**
     * <p>
     * sortByDestroyEffect.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByDestroyEffect(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                ArrayList<String> aKeywords = a.getKeyword();
                ArrayList<String> bKeywords = b.getKeyword();

                boolean aContains = false;
                boolean bContains = false;

                for (String kw : aKeywords) {
                    if (kw.startsWith("Whenever") && kw.contains("into a graveyard from the battlefield,")) {
                        aContains = true;
                        break;
                    }
                }

                for (String kw : bKeywords) {
                    if (kw.startsWith("Whenever") && kw.contains("into a graveyard from the battlefield,")) {
                        bContains = true;
                        break;
                    }
                }
                if (aContains && bContains) {
                    return 0;
                } else if (aContains) {
                    return 1;
                } else if (bContains) {
                    return -1;
                }

                return 0;
            }
        };
        list.sort(com);
    }

    /**
     * <p>
     * sortByIndestructible.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByIndestructible(final CardList list) {
        final ArrayList<String> arrList = new ArrayList<String>();
        arrList.add("Timber Protector");
        arrList.add("Eldrazi Monument");

        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                if (arrList.contains(a.getName()) && arrList.contains(b.getName())) {
                    return 0;
                } else if (arrList.contains(a.getName())) {
                    return 1;
                } else if (arrList.contains(b.getName())) {
                    return -1;
                }

                return 0;
            }
        };
        list.sort(com);
    }

    /**
     * <p>
     * sortByTapped.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByTapped(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {

                if (a.isTapped() && b.isTapped()) {
                    return 0;
                } else if (a.isTapped()) {
                    return 1;
                } else if (b.isTapped()) {
                    return -1;
                }

                return 0;
            }
        };
        list.sort(com);
    }

    /**
     * <p>
     * sortByName.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByName(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                String aName = a.getName();
                String bName = b.getName();

                return aName.compareTo(bName);
            }

        };
        list.sort(com);
    }

    /**
     * <p>
     * sortBySelectable.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param type
     *            a {@link java.lang.String} object.
     */
    public static void sortBySelectable(final CardList list, final String type) {
        final String t = type;
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                if (a.isType(t) && b.isType(t)) {
                    return 0;
                } else if (a.hasKeyword(t)) {
                    return 1;
                } else if (b.hasKeyword(t)) {
                    return -1;
                }

                return 0;
            }
        };
        list.sort(com);
    }

    /**
     * <p>
     * sortByTextLen.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByTextLen(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                int aLen = a.getText().length();
                int bLen = b.getText().length();

                if (aLen == bLen) {
                    return 0;
                } else if (aLen > bLen) {
                    return 1;
                } else if (bLen > aLen) {
                    return -1;
                }

                return 0;
            }
        };
        list.sort(com);
    }

    // Sorts from high to low
    /**
     * <p>
     * sortCMC.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortCMC(final CardList list) {
        Comparator<Card> com = new Comparator<Card>() {
            public int compare(final Card a, final Card b) {
                int cmcA = CardUtil.getConvertedManaCost(a.getManaCost());
                int cmcB = CardUtil.getConvertedManaCost(b.getManaCost());

                if (cmcA == cmcB) {
                    return 0;
                }
                if (cmcA > cmcB) {
                    return -1;
                }
                if (cmcB > cmcA) {
                    return 1;
                }

                return 0;
            }
        };
        list.sort(com);
    } // sortCMC

    /**
     * <p>
     * getColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getColor(final CardList list, final String color) {
        return list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return CardUtil.getColors(c).contains(color);
            }
        });
    } // getColor()

    /**
     * <p>
     * getGoldCards.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getGoldCards(final CardList list) {
        return list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return CardUtil.getColors(c).size() >= 2;
            }
        });
    }

    /**
     * <p>
     * sumAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int sumAttack(final CardList c) {
        int attack = 0;

        for (int i = 0; i < c.size(); i++) {
            // if(c.get(i).isCreature() && c.get(i).hasSecondStrike()) {
            if (c.get(i).isCreature()
                    && (!c.get(i).hasFirstStrike() || (c.get(i).hasDoubleStrike() && c.get(i).hasFirstStrike()))) {
                attack += c.get(i).getNetCombatDamage();
            }
        }
        // System.out.println("Total attack: " +attack);
        return attack;
    } // sumAttack()

    /**
     * <p>
     * sumDefense.
     * </p>
     * 
     * @param c
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int sumDefense(final CardList c) {
        int defense = 0;

        for (int i = 0; i < c.size(); i++) {
            // if(c.get(i).isCreature() && c.get(i).hasSecondStrike()) {
            if (c.get(i).isCreature()) {
                defense += c.get(i).getNetDefense();
            }
        }
        // System.out.println("Total attack: " +attack);
        return defense;
    } // sumAttack()

    /**
     * <p>
     * sumFirstStrikeAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int sumFirstStrikeAttack(final CardList c) {
        int attack = 0;

        for (int i = 0; i < c.size(); i++) {
            if (c.get(i).isCreature() && (c.get(i).hasFirstStrike() || c.get(i).hasDoubleStrike())) {
                attack += c.get(i).getNetCombatDamage();
            }
        }
        Log.debug("Total First Strike attack: " + attack);
        return attack;
    } // sumFirstStrikeAttack()

    // Get the total converted mana cost of a card list
    /**
     * <p>
     * sumCMC.
     * </p>
     * 
     * @param c
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int sumCMC(final CardList c) {
        int cmc = 0;

        for (int i = 0; i < c.size(); i++) {
            cmc += CardUtil.getConvertedManaCost(c.get(i).getManaCost());
        }
        // System.out.println("Total CMC: " +cmc);

        return cmc;

    } // sumCMC

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param c
     *            CardList
     * @param amount
     *            int
     * @return CardList
     */
    public static CardList getRandomSubList(final CardList c, final int amount) {
        if (c.size() < amount) {
            return null;
        }

        CardList subList = new CardList();
        while (subList.size() < amount) {
            c.shuffle();
            subList.add(c.get(0));
            c.remove(0);
        }
        return subList;
    }
}
