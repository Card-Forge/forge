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
package forge.card.cardfactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import forge.AllZone;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardList;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

/**
 * <p>
 * AbstractCardFactory class.
 * </p>
 * 
 * TODO The map field contains Card instances that have not gone through
 * getCard2, and thus lack abilities. However, when a new Card is requested via
 * getCard, it is this map's values that serve as the templates for the values
 * it returns. This class has another field, allCards, which is another copy of
 * the card database. These cards have abilities attached to them, and are owned
 * by the human player by default. <b>It would be better memory-wise if we had
 * only one or the other.</b> We may experiment in the future with using
 * allCard-type values for the map instead of the less complete ones that exist
 * there today.
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbstractCardFactory implements CardFactoryInterface {
    /**
     * This maps card name Strings to Card instances. The Card instances have no
     * owner, and lack abilities.
     * 
     * To get a full-fledged card, see allCards field or the iterator method.
     */
    private final Map<String, Card> map = new TreeMap<String, Card>();

    /** This is a special list of cards, with all abilities attached. */
    protected List<Card> allCardsReadOnly;

    private Set<String> removedCardList;
    private final Card blankCard = new Card(); // new code

    private final CardList copiedList = new CardList();

    /**
     * <p>
     * Constructor for CardFactory.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     */
    protected AbstractCardFactory(final File file) {
        final SpellAbility spell = new SpellAbility(SpellAbility.getSpell(), this.blankCard) {
            // neither computer nor human play can play this card
            @Override
            public boolean canPlay() {
                return false;
            }

            @Override
            public void resolve() {
            }
        };
        this.blankCard.addSpellAbility(spell);
        spell.setManaCost("1");
        this.blankCard.setName("Removed Card");

        // owner and controller will be wrong sometimes
        // but I don't think it will matter
        // theoretically blankCard will go to the wrong graveyard
        this.blankCard.setOwner(AllZone.getHumanPlayer());

        this.removedCardList = new TreeSet<String>(FileUtil.readFile(ForgeProps.getFile(NewConstants.REMOVED)));

    } // constructor

    /**
     * Getter for allCards.
     * 
     * @return allCards
     */
    protected abstract List<Card> getAllCards();

    /**
     * Getter for map.
     * 
     * @return map
     */
    protected final Map<String, Card> getMap() {
        return this.map;
    }

    /**
     * <p>
     * copyCard.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card copyCard(final Card in) {
        final CardCharactersticName curState = in.getCurState();
        if (in.isInAlternateState()) {
            in.setState(CardCharactersticName.Original);
        }
        final Card out = this.getCard(in.getName(), in.getOwner());
        out.setUniqueNumber(in.getUniqueNumber());
        out.setCurSetCode(in.getCurSetCode());

        CardFactoryUtil.copyCharacteristics(in, out);
        if (in.hasAlternateState()) {
            for (final CardCharactersticName state : in.getStates()) {
                in.setState(state);
                if (state == CardCharactersticName.Cloner) {
                    out.addAlternateState(state);
                }
                out.setState(state);
                CardFactoryUtil.copyCharacteristics(in, out);
            }
            in.setState(curState);
            out.setState(curState);
        }

        // I'm not sure if we really should be copying enchant/equip stuff over.
        out.setEquipping(in.getEquipping());
        out.setEquippedBy(in.getEquippedBy());
        out.setEnchantedBy(in.getEnchantedBy());
        out.setEnchanting(in.getEnchanting());
        out.setClones(in.getClones());
        for (final Object o : in.getRemembered()) {
            out.addRemembered(o);
        }
        for (final Card o : in.getImprinted()) {
            out.addImprinted(o);
        }

        return out;

    }

    /**
     * <p>
     * copyCardintoNew.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card copyCardintoNew(final Card in) {
        final Card out = CardFactoryUtil.copyStats(in);
        out.setOwner(in.getOwner());
        out.setCopiedSpell(true);
        this.copiedList.add(out);
        return out;
    }

    /**
     * <p>
     * copySpellontoStack.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param original
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param bCopyDetails
     *            a boolean.
     */
    @Override
    public final void copySpellontoStack(final Card source, final Card original, final SpellAbility sa,
            final boolean bCopyDetails) {
        Player controller = sa.getActivatingPlayer();
        /*if (sa.getPayCosts() == null) {
            this.copySpellontoStack(source, original, bCopyDetails);
            return;
        }*/
        final Card c = AllZone.getCardFactory().copyCard(original);
        c.addController(controller);
        c.setCopiedSpell(true);

        final SpellAbility copySA = sa.copy();
        if (sa.getTarget() != null) {
            Target target = new Target(sa.getTarget());
            target.setSourceCard(c);
            copySA.setTarget(target);
            /*if (copySA.getAbilityFactory() != null) {
                AbilityFactory af = new AbilityFactory(sa.getAbilityFactory());
                af.setAbTgt(target);
                af.setHostCard(source);
                copySA.setAbilityFactory(af);
            }*/
        }
        copySA.setSourceCard(c);

        if (bCopyDetails) {
            c.addXManaCostPaid(original.getXManaCostPaid());
            c.addMultiKickerMagnitude(original.getMultiKickerMagnitude());
            if (original.isKicked()) {
                c.setKicked(true);
            }
            c.addReplicateMagnitude(original.getReplicateMagnitude());
            if (sa.isReplicate()) {
                copySA.setIsReplicate(true);
            }
        }

        if (controller.isHuman()) {
            Singletons.getModel().getGameAction().playSpellAbilityForFree(copySA);
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(copySA);
        }
    }

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     * @param owner
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.Card} instance, owned by owner; or the special
     *         blankCard
     */
    @Override
    public final Card getCard(final String cardName, final Player owner) {
        if (this.removedCardList.contains(cardName) || cardName.equals(this.blankCard.getName())) {
            return this.blankCard;
        }

        //System.out.println(cardName);
        return this.getCard2(cardName, owner);
    }

    protected Card getCard2(final String cardName, final Player owner) {
        // o should be Card object
        final Card o = this.map.get(cardName);
        if (o == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("CardFactory : getCard() invalid card name - ").append(cardName);
            throw new RuntimeException(sb.toString());
        }

        return getCard2(o, owner);
    }

    public static Card getCard2(final Card o, final Player owner) {
        final Card copy = CardFactoryUtil.copyStats(o);
        copy.setOwner(owner);
        return buildAbilities(copy);
    }

    public static Card buildAbilities(final Card card) {
        final String cardName = card.getName();

        if (!card.isCardColorsOverridden()) {
            card.addColor(card.getManaCost().toString());
        }
        // may have to change the spell

        // this is so permanents like creatures and artifacts have a "default"
        // spell
        if (card.isPermanent() && !card.isLand() && !card.isAura()) {
            card.addSpellAbility(new SpellPermanent(card));
        }

        CardFactoryUtil.parseKeywords(card, cardName);

        for (final CardCharactersticName state : card.getStates()) {
            if (card.isDoubleFaced() && state == CardCharactersticName.FaceDown) {
                continue; // Ignore FaceDown for DFC since they have none.
            }
            card.setState(state);
            CardFactoryUtil.addAbilityFactoryAbilities(card);
            final ArrayList<String> stAbs = card.getStaticAbilityStrings();
            if (stAbs.size() > 0) {
                for (int i = 0; i < stAbs.size(); i++) {
                    card.addStaticAbility(stAbs.get(i));
                }
            }
        }

        card.setState(CardCharactersticName.Original);

        // ******************************************************************
        // ************** Link to different CardFactories *******************
        Card card2 = null;
        if (card.isCreature()) {
            card2 = CardFactoryCreatures.getCard(card, cardName);
        } else if (card.isAura()) {
            card2 = CardFactoryAuras.getCard(card, cardName);
        } else if (card.isEquipment()) {
            card2 = CardFactoryEquipment.getCard(card, cardName);
        } else if (card.isPlaneswalker()) {
            card2 = CardFactoryPlaneswalkers.getCard(card, cardName);
        } else if (card.isLand()) {
            card2 = CardFactoryLands.getCard(card, cardName);
        } else if (card.isInstant()) {
            card2 = CardFactoryInstants.getCard(card, cardName);
        } else if (card.isSorcery()) {
            card2 = CardFactorySorceries.getCard(card, cardName);
        } else if (card.isEnchantment()) {
            card2 = CardFactoryEnchantments.getCard(card, cardName);
        } else if (card.isArtifact()) {
            card2 = CardFactoryArtifacts.getCard(card, cardName);
        }

        return CardFactoryUtil.postFactoryKeywords(card2 != null ? card2 : card);
    } // getCard2
} // end class AbstractCardFactory
