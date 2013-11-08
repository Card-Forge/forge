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

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CounterType;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/**
 * <p>
 * CardFactory_Creatures class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryCreatures {

    private static void getCard_SphinxJwar(final Card card) {
        final SpellAbility ability1 = new AbilityStatic(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                final Player player = card.getController();
                final PlayerZone lib = player.getZone(ZoneType.Library);

                if (lib.size() < 1 || !this.getActivatingPlayer().equals(card.getController())) {
                    return;
                }

                final List<Card> cl = new ArrayList<Card>();
                cl.add(lib.get(0));

                GuiChoose.oneOrNone("Top card", cl);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }
        }; // SpellAbility

        final StringBuilder sb1 = new StringBuilder();
        sb1.append(card.getName()).append(" - look at top card of library.");
        ability1.setStackDescription(sb1.toString());

        ability1.setDescription("You may look at the top card of your library.");
        card.addSpellAbility(ability1);
    }

    public static void buildCard(final Card card, final String cardName) {

        if (cardName.equals("Sphinx of Jwar Isle")) {
            getCard_SphinxJwar(card);
        }

        // ***************************************************
        // end of card specific code
        // ***************************************************

        final int iLvlUp = CardFactoryUtil.hasKeyword(card, "Level up");
        final int iLvlMax = CardFactoryUtil.hasKeyword(card, "maxLevel");
        
        if (iLvlUp != -1 && iLvlMax != -1) {
            final String parse = card.getKeyword().get(iLvlUp);
            final String parseMax = card.getKeyword().get(iLvlMax);
            card.addSpellAbility(makeLevellerAbility(card, parse, parseMax));
            card.setLevelUp(true);
        } // level up
    }


    private static SpellAbility makeLevellerAbility(final Card card, final String strLevelCost, final String strMaxLevel) {
        card.removeIntrinsicKeyword(strLevelCost);
        card.removeIntrinsicKeyword(strMaxLevel);

        final String[] k = strLevelCost.split(":");
        final String manacost = k[1];

        final String[] l = strMaxLevel.split(":");
        final int maxLevel = Integer.parseInt(l[1]);

        class LevelUpAbility extends AbilityActivated {
            public LevelUpAbility(final Card ca, final String s) {
                super(ca, new Cost(manacost, true), null);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated levelUp = new LevelUpAbility(getSourceCard(), getPayCosts().toString());
                levelUp.getRestrictions().setSorcerySpeed(true);
                return levelUp;
            }

            private static final long serialVersionUID = 3998280279949548652L;

            @Override
            public void resolve() {
                card.addCounter(CounterType.LEVEL, 1, true);
            }

            @Override
            public boolean canPlayAI() {
                // creatures enchanted by curse auras have low priority
                if (card.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                    for (Card aura : card.getEnchantedBy()) {
                        if (aura.getController().isOpponentOf(card.getController())) {
                            return false;
                        }
                    }
                }
                return card.getCounters(CounterType.LEVEL) < maxLevel;
            }

            @Override
            public String getDescription() {
                final StringBuilder sbDesc = new StringBuilder();
                sbDesc.append("Level up ").append(manacost).append(" (").append(manacost);
                sbDesc.append(": Put a level counter on this. Level up only as a sorcery.)");
                return sbDesc.toString();
            }
        }
        final SpellAbility levelUp = new LevelUpAbility(card, manacost);
        levelUp.getRestrictions().setSorcerySpeed(true);
        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(card).append(" - put a level counter on this.");
        levelUp.setStackDescription(sbStack.toString());
        return levelUp;
    }
}
