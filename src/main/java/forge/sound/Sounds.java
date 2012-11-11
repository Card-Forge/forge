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
 * Sounds (enumeration) - all sounds in the game must be declared here.
 * Once declared, the sound can be played from anywhere in the code
 * using Sounds.soundName.play(). The sounds are only preloaded once,
 * so there is no memory overhead for playing the sound multiple times.
 * 
 * Currently, if the file does not exist, it is not a fatal error. 
 * No sound is played in that case, a simple message is generated on
 * the debug console during preloading.
 * 
 * @author Agetian
 */
public enum Sounds {
    Tap("res/sound/tap.wav");

    SoundSystem snd = null;
    
    Sounds(String filename) {
        snd = new SoundSystem(filename);
    }

    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play() {
        snd.play();
    }

    /**
     * Stop the sound associated with the Sounds enumeration element.
     */
    public void stop() {
        snd.stop();
    }
}
