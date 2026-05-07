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
package forge.gamemodes.limited;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.item.IPaperCard;

/**
 * DeckColors variant for Commander Draft.
 * Colors are locked exclusively to the drafted commander(s) color identity;
 * no free accumulation is permitted.
 */
public class CommanderDeckColors extends FullDeckColors {

    private boolean locked = false;

    public CommanderDeckColors() {
        super();
    }

    /**
     * Lock this deck's color identity to match the given commander's color identity.
     * Must be called once after the primary commander is chosen.
     *
     * @param identity the commander's color identity
     */
    public void lockToColorIdentity(final ColorSet identity) {
        colorMask = 0;
        for (final byte color : MagicColor.WUBRG) {
            if (identity.hasAnyColor(color)) {
                colorMask |= color;
            }
        }
        chosen = null;
        locked = true;
    }

    /**
     * Supplement the locked color identity with the partner commander's color identity.
     * Should be called once after picking a partner commander.
     *
     * @param partner the partner commander card
     */
    public void addColorIdentityOfPartner(final IPaperCard partner) {
        final ColorSet partnerIdentity = partner.getRules().getColorIdentity();
        for (final byte color : MagicColor.WUBRG) {
            if (partnerIdentity.hasAnyColor(color)) {
                colorMask |= color;
            }
        }
        chosen = null;
        locked = true;
    }

    /**
     * Colors are determined solely by the commander(s); never accumulate freely.
     */
    @Override
    public void addColorsOf(final IPaperCard pickedCard) {
        // No-op: color identity is set only via lockToColorIdentity / addColorIdentityOfPartner
    }

    /**
     * Once locked, there are no more colors to add.
     * Before locking (no commander yet), also return false so the ranking logic
     * does not try to accumulate colors during the commander-selection phase.
     */
    @Override
    public boolean canChoseMoreColors() {
        return false;
    }

    /** Whether this deck's colors have been locked to a commander's identity. */
    public boolean isLocked() {
        return locked;
    }
}

