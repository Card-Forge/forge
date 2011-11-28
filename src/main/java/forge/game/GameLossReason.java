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
package forge.game;

/**
 * The Enum GameLossReason.
 */
public enum GameLossReason {

    /** The Did not lose yet. */
    DidNotLoseYet, // a winner must have this status by the end of the game

    /** The Conceded. */
    Conceded, // rule 104.3a
    /** The Life reached zero. */
    LifeReachedZero, // rule 104.3b
    /** The Milled. */
    Milled, // 104.3c
    /** The Poisoned. */
    Poisoned, // 104.3d

    // 104.3e and others
    /** The Spell effect. */
    SpellEffect

    /*
     * DoorToNothingness, // Door To Nothingness's ability activated
     * 
     * // TODO: Implement game logics for the ones below Transcendence20Life, //
     * When you have 20 or more life, you lose the game. FailedToPayPactUpkeep,
     * // Pacts from Future Sight series (cost 0 but you must pay their real
     * cost at next turn's upkeep, otherwise GL) PhageTheUntouchableDamage, //
     * Whenever Phage deals combat damage to a player, that player loses the
     * game. PhageTheUntouchableWrongETB, // When Phage the Untouchable ETB, if
     * you didn't cast it from your hand, you lose the game.
     * NefariousLichLeavesTB, // When Nefarious Lich leaves the battlefield, you
     * lose the game. NefariousLichCannotExileGrave, // If damage would be dealt
     * to you, exile that many cards from your graveyard instead. If you can't,
     * you lose the game. LichWasPutToGraveyard, // When Lich is put into a
     * graveyard from the battlefield, you lose the game. FinalFortune, // same
     * as Warrior's Oath - lose at the granted extra turn's end step
     * ImmortalCoilEmptyGraveyard, // When there are no cards in your graveyard,
     * you lose the game. ForbiddenCryptEmptyGraveyard, // If you would draw a
     * card, return a card from your graveyard to your hand instead. If you
     * can't, you lose the game.
     * 
     * // Amulet of quoz skipped for using ante, // Form of the Squirrel and
     * Rocket-Powered Turbo Slug skipped for being part of UN- set
     */

    // refer to
    // http://gatherer.wizards.com/Pages/Search/Default.aspx?output=standard&text=+[%22lose+the+game%22]
    // for more cards when they are printed
}
