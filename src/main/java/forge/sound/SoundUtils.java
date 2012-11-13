/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2012  Forge Team
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

package forge.sound;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.List;

/**
 * SoundUtils - static methods for sound playback involving more than a simple
 * call to Sounds.soundName.play.
 * 
 * @author Agetian
 */
public class SoundUtils {

    /**
     * Plays the sound corresponding to the card type/color when the card
     * ability resolves on the stack.
     *
     * @param source the card to play the sound for.
     * @param sa the spell ability that was resolving.
     */
    public static void playCardSoundEffect(final Card source, final SpellAbility sa) {
        if (sa == null || source == null) {
            return;
        }
        
        if (sa.isSpell()) {
            if (source.isCreature() && source.isArtifact()) {
                Sounds.ArtifactCreature.play();
            } else if (source.isCreature()) {
                Sounds.Creature.play();
            } else if (source.isArtifact()) {
                Sounds.Artifact.play();
            } else if (source.isPlaneswalker()) {
                Sounds.Planeswalker.play();
            } else if (source.isEnchantment()) {
                Sounds.Enchantment.play();
            }
        }
    }

    /**
     * Plays the sound corresponding to the land type when the land is played.
     * 
     * @param land the land card that was played
     */
    public static void playLandSoundEffect(final Card land) {
        if (land == null)
            return;

        final List<SpellAbility> manaProduced = land.getManaAbility();
        boolean effectPlayed = false;
        
        for (SpellAbility sa : manaProduced) {
            String manaColors = sa.getManaPart().getManaProduced();

            if (manaColors.contains("B")) {
                Sounds.BlackLand.play();
                effectPlayed = true;
            }
            if (manaColors.contains("U")) {
                Sounds.BlueLand.play();
                effectPlayed = true;
            }
            if (manaColors.contains("G")) {
                Sounds.GreenLand.play();
                effectPlayed = true;
            }
            if (manaColors.contains("R")) {
                Sounds.RedLand.play();
                effectPlayed = true;
            }
            if (manaColors.contains("W")) {
                Sounds.WhiteLand.play();
                effectPlayed = true;
            }
        }

        // play a generic land sound if no other sound corresponded to it.
        if (!effectPlayed) {
            Sounds.OtherLand.play();
        }
    }
}
