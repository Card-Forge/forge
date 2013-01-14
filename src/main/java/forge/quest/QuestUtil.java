/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.quest;

import forge.Card;

import forge.card.cardfactory.CardFactory;
import forge.game.player.Player;
import forge.item.CardDb;
import forge.quest.bazaar.QuestPetController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * QuestUtil class.
 * </p>
 * MODEL - Static utility methods to help with minor tasks around Quest.
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestUtil {
    /**
     * <p>
     * getComputerStartingCards.
     * </p>
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getComputerStartingCards() {
        return new ArrayList<Card>();
    }

    /**
     * <p>
     * getComputerStartingCards.
     * </p>
     * Returns new card instances of extra AI cards in play at start of event.
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     * @param qe
     *            a {@link forge.quest.QuestEvent} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getComputerStartingCards(final QuestEvent qe, Player ai) {
        final List<Card> list = new ArrayList<Card>();

        for (final String s : qe.getAiExtraCards()) {
            list.add(QuestUtil.readExtraCard(s, ai));
        }

        return list;
    }

    /**
     * <p>
     * getHumanStartingCards.
     * </p>
     * Returns list of current plant/pet configuration only.
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getHumanStartingCards(final QuestController qc) {
        final List<Card> list = new ArrayList<Card>();

        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {

            String petName = qc.getSelectedPet(iSlot);
            QuestPetController pet = qc.getPetsStorage().getPet(petName);
            if (pet != null) {
                Card c = pet.getPetCard(qc.getAssets());
                if (c != null) {
                    Card copy = CardFactory.getCard2(c, null);
                    copy.setSickness(true);
                    copy.setImageName(c.getImageName());
                    copy.setToken(true);
                    list.add(copy);
                }
            }
        }

        return list;
    }

    /**
     * <p>
     * getHumanStartingCards.
     * </p>
     * Returns new card instances of extra human cards, including current
     * plant/pet configuration, and cards in play at start of quest.
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     * @param qe
     *            a {@link forge.quest.QuestEvent} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getHumanStartingCards(final QuestController qc, final QuestEvent qe, final Player human) {
        final List<Card> list = QuestUtil.getHumanStartingCards(qc);
        for (final String s : qe.getHumanExtraCards()) {
            list.add(QuestUtil.readExtraCard(s, human));
        }
        return list;
    }

    /**
     * <p>
     * createToken.
     * </p>
     * Creates a card instance for token defined by property string.
     * 
     * @param s
     *            Properties string of token
     *            (TOKEN;W;1;1;sheep;type;type;type...)
     * @return token Card
     */
    public static Card createToken(final String s) {
        final String[] properties = s.split(";");
        final Card c = new Card();
        c.setToken(true);

        // c.setManaCost(properties[1]);
        c.addColor(properties[1]);
        c.setBaseAttack(Integer.parseInt(properties[2]));
        c.setBaseDefense(Integer.parseInt(properties[3]));
        c.setName(properties[4]);

        c.setImageName(properties[1] + " " + properties[2] + " " + properties[3] + " " + properties[4]);

        int x = 5;
        while (x != properties.length) {
            c.addType(properties[x++]);
        }

        return c;
    }

    /**
     * <p>
     * readExtraCard.
     * </p>
     * Creates single card for a string read from unique event properties.
     * 
     * @param name
     *            the name
     * @param owner
     *            the owner
     * @return the card
     */
    public static Card readExtraCard(final String name, Player owner) {
        // Token card creation
        Card tempcard;
        if (name.startsWith("TOKEN")) {
            tempcard = QuestUtil.createToken(name);
            tempcard.setOwner(owner);
            return tempcard;
        }
        // Standard card creation
        return CardDb.instance().getCard(name, true).toForgeCard(owner);
    }

} // QuestUtil
