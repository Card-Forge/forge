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
 * loaded, the sound can be played from the code by posting the appropriate
 * event to the event bus via Singletons.getModel().getGame().getEvents().post.
 *
 * The second parameter specifies whether a sound needs to be synced with other
 * similar sounds (when there's a chance of multiple instances of the same sound
 * generated in quick succession, so that the slowdown can be avoided) or not.
 * 
 * Currently, if the file does not exist, it is not a fatal error. No sound is
 * played in that case, a simple message is generated on the debug console
 * during preloading.
 *
 * @author Agetian
 */
public enum SoundEffectType {
    // Sounds must be listed in alphabetic order.

    AddCounter("add_counter.mp3", true),
    Artifact("artifact.mp3", false),
    ArtifactCreature("artifact_creature.mp3", false),
    BlackLand("black_land.mp3", false),
    BlackRedGreenLand("black_red_green_land.mp3", false),
    BlackRedLand("black_red_land.mp3", false),
    BlackWhiteGreenLand("black_white_green_land.mp3", false),
    BlackWhiteLand("black_white_land.mp3", false),
    Block("block.mp3", false),
    BlueBlackLand("blue_black_land.mp3", false),
    BlueBlackRedLand("blue_black_red_land.mp3", false),
    BlueLand("blue_land.mp3", false),
    Creature("creature.mp3", false),
    Damage("damage.mp3", true),
    Daytime("daytime.mp3", true),
    Destroy("destroy.mp3", true),
    Discard("discard.mp3", false),
    Draw("draw.mp3", false),
    Enchantment("enchant.mp3", false),
    EndOfTurn("end_of_turn.mp3", false),
    Equip("equip.mp3", false),
    Exile("exile.mp3", false),
    FlipCoin("flip_coin.mp3", false),
    GreenBlackBlueLand("green_black_blue_land.mp3", false),
    GreenBlackLand("green_black_land.mp3", false),
    GreenBlueLand("green_blue_land.mp3", false),
    GreenBlueRedLand("green_blue_red_land.mp3", false),
    GreenLand("green_land.mp3", false),
    GreenRedLand("green_red_land.mp3", false),
    GreenRedWhiteLand("green_red_white_land.mp3", false),
    Instant("instant.mp3", false),
    LifeGain("life_gain.mp3", true),
    LifeLoss("life_loss.mp3", true),
    LoseDuel("lose_duel.mp3", false),
    ManaBurn("mana_burn.mp3", false),
    Nighttime("nighttime.mp3", true),
    OtherLand("other_land.mp3", false),
    Phasing("phasing.mp3", true),
    Planeswalker("planeswalker.mp3", false),
    Poison("poison.mp3", true),
    RedBlueLand("red_blue_land.mp3", false),
    RedBlueWhiteLand("red_blue_white_land.mp3", false),
    RedLand("red_land.mp3", false),
    Regen("regeneration.mp3", false),
    RemoveCounter("remove_counter.mp3", true),
    RollDie("roll_die.mp3", false),
    Sacrifice("sacrifice.mp3", true),
    ScriptedEffect("", false), // Plays the effect defined by SVar:SoundEffect
    Shuffle("shuffle.mp3", false),
    Sorcery("sorcery.mp3", false),
    StartDuel("start_duel.mp3",false),
    Tap("tap.mp3", false),
    Token("token.mp3", true),
    Untap("untap.mp3", true),
    WhiteBlueBlackLand("white_blue_black_land.mp3", false),
    WhiteBlueLand("white_blue_land.mp3", false),
    WhiteGreenBlueLand("white_green_blue_land.mp3", false),
    WhiteGreenLand("white_green_land.mp3", false),
    WhiteLand("white_land.mp3", false),
    WhiteRedBlackLand("white_red_black_land.mp3", false),
    WhiteRedLand("white_red_land.mp3", false),
    WinDuel("win_duel.mp3", false);

    private final String resourceFileName;
    private final boolean isSync;

    /**
     * @return the resourceFileName
     */
    public String getResourceFileName() {
        return resourceFileName;
    }
    /**
     * @param filename
     *              name of the sound file associated with the entry.
     * @param isSoundSynced
     *              determines if only one instance of the sound can be played
     *              at a time (the sound is synced with the other sounds of the
     *              same kind).
     */
    SoundEffectType(final String filename, final boolean isSoundSynced) {
        resourceFileName = filename;
        isSync = isSoundSynced;
    }

    /**
     * determine if the sound effect needs to be synchronized with the other
     * events of the same kind or not.
     * 
     * @return true if the sound effect can only be played if no other sound
     * of the same kind is already playing.
     */
    public boolean isSynced() {
        return isSync;
    }
}
