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

    AddCounter("add_counter.wav", true),
    Artifact("artifact.wav", false),
    ArtifactCreature("artifact_creature.wav", false),
    BlackLand("black_land.wav", false),
    BlackRedGreenLand("black_red_green_land.wav", false),
    BlackRedLand("black_red_land.wav", false),
    BlackWhiteGreenLand("black_white_green_land.wav", false),
    BlackWhiteLand("black_white_land.wav", false),
    Block("block.wav", false),
    BlueBlackLand("blue_black_land.wav", false),
    BlueBlackRedLand("blue_black_red_land.wav", false),
    BlueLand("blue_land.wav", false),
    Creature("creature.wav", false),
    Damage("damage.wav", true),
    Destroy("destroy.wav", true),
    Discard("discard.wav", false),
    Draw("draw.wav", false),
    Enchantment("enchant.wav", false),
    EndOfTurn("end_of_turn.wav", false),
    Equip("equip.wav", false),
    Exile("exile.wav", false),
    FlipCoin("flip_coin.wav", false),
    GreenBlackBlueLand("green_black_blue_land.wav", false),
    GreenBlackLand("green_black_land.wav", false),
    GreenBlueLand("green_blue_land.wav", false),
    GreenBlueRedLand("green_blue_red_land.wav", false),
    GreenLand("green_land.wav", false),
    GreenRedLand("green_red_land.wav", false),
    GreenRedWhiteLand("green_red_white_land.wav", false),
    Instant("instant.wav", false),
    LifeGain("life_gain.wav", true),
    LifeLoss("life_loss.wav", true),
    LoseDuel("lose_duel.wav", false),
    ManaBurn("mana_burn.wav", false),
    OtherLand("other_land.wav", false),
    Phasing("phasing.wav", true),
    Planeswalker("planeswalker.wav", false),
    Poison("poison.wav", true),
    RedBlueLand("red_blue_land.wav", false),
    RedBlueWhiteLand("red_blue_white_land.wav", false),
    RedLand("red_land.wav", false),
    Regen("regeneration.wav", false),
    RemoveCounter("remove_counter.wav", true),
    Sacrifice("sacrifice.wav", true),
    ScriptedEffect("", false), // Plays the effect defined by SVar:SoundEffect
    Shuffle("shuffle.wav", false),
    Sorcery("sorcery.wav", false),
    StartDuel("start_duel.wav",false),
    Tap("tap.wav", false),
    Token("token.wav", true),
    Untap("untap.wav", true),
    WhiteBlueBlackLand("white_blue_black_land.wav", false),
    WhiteBlueLand("white_blue_land.wav", false),
    WhiteGreenBlueLand("white_green_blue_land.wav", false),
    WhiteGreenLand("white_green_land.wav", false),
    WhiteLand("white_land.wav", false),
    WhiteRedBlackLand("white_red_black_land.wav", false),
    WhiteRedLand("white_red_land.wav", false),
    WinDuel("win_duel.wav", false);

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
