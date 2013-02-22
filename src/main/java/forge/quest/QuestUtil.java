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

import forge.card.CardEdition;
import forge.card.CardRulesReader;
import forge.item.CardDb;
import forge.item.CardToken;
import forge.item.IPaperCard;
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
    public static List<IPaperCard> getComputerStartingCards(final QuestEvent qe) {
        final List<IPaperCard> list = new ArrayList<IPaperCard>();

        for (final String s : qe.getAiExtraCards()) {
            list.add(QuestUtil.readExtraCard(s));
        }

        return list;
    }

    /**
     * <p>
     * getHumanStartingCards.
     * </p>
     * Returns list of current plant/pet configuration only.
     * @param human 
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<IPaperCard> getHumanStartingCards(final QuestController qc) {
        final List<IPaperCard> list = new ArrayList<IPaperCard>();

        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {
            String petName = qc.getSelectedPet(iSlot);
            QuestPetController pet = qc.getPetsStorage().getPet(petName);
            if (pet != null) {
                IPaperCard c = pet.getPetCard(qc.getAssets());
                if (c != null) {
                    list.add(c);
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
    public static List<IPaperCard> getHumanStartingCards(final QuestController qc, final QuestEvent qe) {
        final List<IPaperCard> list = QuestUtil.getHumanStartingCards(qc);
        for (final String s : qe.getHumanExtraCards()) {
            list.add(QuestUtil.readExtraCard(s));
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
    public static CardToken createToken(final String s) {
        final String[] properties = s.split(";", 5);

        List<String> script = new ArrayList<String>();
        script.add("Name:" + properties[4]);
        script.add("Colors:" + properties[1]);
        script.add("PT:"+ properties[2] + "/" + properties[3]);
        script.add("Types:" + properties[5].replace(';', ' '));
        // c.setManaCost(properties[1]);
        String fileName = properties[1] + " " + properties[2] + " " + properties[3] + " " + properties[4];
        final CardToken c = new CardToken(CardRulesReader.parseSingleCard(script), CardEdition.UNKNOWN.getCode(), fileName);
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
    public static IPaperCard readExtraCard(final String name) {
        // Token card creation
        IPaperCard tempcard;
        if (name.startsWith("TOKEN")) {
            tempcard = QuestUtil.createToken(name);
            return tempcard;
        }
        // Standard card creation
        return CardDb.instance().getCard(name, true);
    }

} // QuestUtil
