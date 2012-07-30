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

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.card.cardfactory.CardFactory;
import forge.game.player.Player;
import forge.item.CardDb;
import forge.quest.bazaar.QuestPetController;

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
    public static CardList getComputerStartingCards() {
        return new CardList();
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
    public static CardList getComputerStartingCards(final QuestEvent qe) {
        final CardList list = new CardList();

        if (qe instanceof QuestEventChallenge) {
            final List<String> extras = ((QuestEventChallenge) qe).getAIExtraCards();

            for (final String s : extras) {
                list.add(QuestUtil.readExtraCard(s, AllZone.getComputerPlayer()));
            }
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
    public static CardList getHumanStartingCards(final QuestController qc) {
        final CardList list = new CardList();

        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {

            String petName = qc.getSelectedPet(iSlot);
            QuestPetController pet = qc.getPetsStorage().getPet(petName);
            if (pet != null) {
                Card c = pet.getPetCard(qc.getAssets());
                if (c != null) {
                    Card copy = CardFactory.getCard2(c, AllZone.getHumanPlayer());
                    copy.setSickness(true);
                    copy.addController(AllZone.getHumanPlayer());
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
    public static CardList getHumanStartingCards(final QuestController qc, final QuestEvent qe) {
        final CardList list = QuestUtil.getHumanStartingCards(qc);

        if (qe instanceof QuestEventChallenge) {
            final List<String> extras = ((QuestEventChallenge) qe).getHumanExtraCards();

            for (final String s : extras) {
                list.add(QuestUtil.readExtraCard(s, AllZone.getHumanPlayer()));
            }
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
    public static Card readExtraCard(final String name, final Player owner) {
        // Token card creation
        Card tempcard;
        if (name.startsWith("TOKEN")) {
            tempcard = QuestUtil.createToken(name);
            tempcard.addController(owner);
            tempcard.setOwner(owner);
        }
        // Standard card creation
        else {
            tempcard = CardDb.instance().getCard(name, true).toForgeCard(owner);
        }
        return tempcard;
    }

} // QuestUtil
