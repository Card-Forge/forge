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

/**
 * Sounds (enumeration) - all sounds in the game must be declared here. Once
 * declared, the sound can be played from anywhere in the code using
 * Sounds.soundName.play(). The sounds are only preloaded once, so there is no
 * memory overhead for playing the sound multiple times.
 *
 * Currently, if the file does not exist, it is not a fatal error. No sound is
 * played in that case, a simple message is generated on the debug console
 * during preloading.
 *
 * @author Agetian
 */
public enum SoundEffectType {
    // Sounds must be listed in alphabetic order.

    AddCounter("add_counter.wav"),
    Artifact("artifact.wav"),
    ArtifactCreature("artifact_creature.wav"),
    BlackLand("black_land.wav"),
    BlueLand("blue_land.wav"),
    Creature("creature.wav"),
    Damage("damage.wav"), 
    Destroy("destroy.wav"),
    Discard("discard.wav"),
    Draw("draw.wav"),
    Enchantment("enchant.wav"), 
    EndOfTurn("end_of_turn.wav"),
    Equip("equip.wav"),
    FlipCoin("flip_coin.wav"),
    GreenLand("green_land.wav"),
    Instant("instant.wav"),
    LifeLoss("life_loss.wav"),
    LoseDuel("lose_duel.wav"),
    ManaBurn("mana_burn.wav"),
    OtherLand("other_land.wav"),
    Planeswalker("planeswalker.wav"),
    Poison("poison.wav"),
    RedLand("red_land.wav"),
    Regen("regeneration.wav"),
    RemoveCounter("remove_counter.wav"),
    Sacrifice("sacrifice.wav"),
    Sorcery("sorcery.wav"),
    Shuffle("shuffle.wav"),
    Tap("tap.wav"),
    Untap("untap.wav"),
    WhiteLand("white_land.wav"),
    WinDuel("win_duel.wav");

    
    private final String resourceFileName;
    /**
     * @return the resourceFileName
     */
    public String getResourceFileName() {
        return resourceFileName;
    }
    /**
     * @param filename 
     *              name of the sound file associated with the entry.
     */
    SoundEffectType(final String filename) {
        resourceFileName = filename;
    }
}
