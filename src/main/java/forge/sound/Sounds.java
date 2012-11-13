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

import forge.Singletons;
import forge.properties.ForgePreferences.FPref;

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
public enum Sounds {
    // Sounds must be listed in alphabetic order.

    Artifact("res/sound/artifact.wav"),
    ArtifactCreature("res/sound/artifact_creature.wav"),
    BlackLand("res/sound/black_land.wav"),
    BlueLand("res/sound/blue_land.wav"),
    Counter("res/sound/counter.wav"), /* NOT IMPLEMENTED YET */
    Creature("res/sound/creature.wav"),
    Damage("res/sound/damage.wav"), /* NOT IMPLEMENTED YET */
    Discard("res/sound/discard.wav"), /* NOT IMPLEMENTED YET */
    Draw("res/sound/draw.wav"),
    Enchantment("res/sound/enchant.wav"), /* NOT IMPLEMENTED YET */
    EndOfTurn("res/sound/end_of_turn.wav"),
    FlipCoin("res/sound/flip_coin.wav"),
    GreenLand("res/sound/green_land.wav"),
    Instant("res/sound/instant.wav"),
    LifeLoss("res/sound/life_loss.wav"),
    LoseDuel("res/sound/lose_duel.wav"),
    OtherLand("res/sound/other_land.wav"),
    Planeswalker("res/sound/planeswalker.wav"),
    Poison("res/sound/poison.wav"),
    RedLand("res/sound/red_land.wav"),
    Regen("res/sound/regeneration.wav"), /* NOT IMPLEMENTED YET */
    Sacrifice("res/sound/sacrifice.wav"), /* NOT IMPLEMENTED YET */
    Sorcery("res/sound/sorcery.wav"),
    Shuffle("res/sound/shuffle.wav"),
    Tap("res/sound/tap.wav"),
    Untap("res/sound/untap.wav"),
    WhiteLand("res/sound/white_land.wav"),
    WinDuel("res/sound/win_duel.wav");

    private SoundSystem snd = null;
    
    /**
     * @param filename 
     *              name of the sound file associated with the entry.
     */
    Sounds(final String filename) {
        snd = new SoundSystem(filename);
    }

    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play() {
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            snd.play();
        }
    }

    /**
     * Play the sound in a looping manner until 'stop' is called.
     */
    public void loop() {
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            snd.loop();
        }
    }

    /**
     * Stop the sound associated with the Sounds enumeration element.
     */
    public void stop() {
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            snd.stop();
        }
    }
}
