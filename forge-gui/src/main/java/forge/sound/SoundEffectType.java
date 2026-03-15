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

    AddCounter("add_counter", true),
    Artifact("artifact", false),
    ArtifactCreature("artifact_creature", false),
    BlackLand("black_land", false),
    BlackRedGreenLand("black_red_green_land", false),
    BlackRedLand("black_red_land", false),
    BlackWhiteGreenLand("black_white_green_land", false),
    BlackWhiteLand("black_white_land", false),
    Block("block", false),
    BlueBlackLand("blue_black_land", false),
    BlueBlackRedLand("blue_black_red_land", false),
    BlueLand("blue_land", false),
    ButtonPress("button_press", false),
    CoinsDrop("coins_drop", false),
    Creature("creature", false),
    Damage("damage", true),
    Daytime("daytime", true),
    Destroy("destroy", true),
    Discard("discard", false),
    Draw("draw", false),
    Enchantment("enchant", false),
    EndOfTurn("end_of_turn", false),
    Equip("equip", false),
    Exile("exile", false),
    FlipCard("flip_card", false),
    FlipCoin("flip_coin", false),
    GreenBlackBlueLand("green_black_blue_land", false),
    GreenBlackLand("green_black_land", false),
    GreenBlueLand("green_blue_land", false),
    GreenBlueRedLand("green_blue_red_land", false),
    GreenLand("green_land", false),
    GreenRedLand("green_red_land", false),
    GreenRedWhiteLand("green_red_white_land", false),
    Instant("instant", false),
    LifeGain("life_gain", true),
    LifeLoss("life_loss", true),
    LoseDuel("lose_duel", false),
    ManaBurn("mana_burn", false),
    Nighttime("nighttime", true),
    OtherLand("other_land", false),
    Phasing("phasing", true),
    Planeswalker("planeswalker", false),
    Poison("poison", true),
    RedBlueLand("red_blue_land", false),
    RedBlueWhiteLand("red_blue_white_land", false),
    RedLand("red_land", false),
    Regen("regeneration", false),
    RemoveCounter("remove_counter", true),
    RollDie("roll_die", false),
    Sacrifice("sacrifice", true),
    ScriptedEffect("", false), // Plays the effect defined by SVar:SoundEffect
    Shuffle("shuffle", false),
    SnapshotRestored("rewind", false),
    SpeedUp("speedup", false),
    Sorcery("sorcery", false),
    Sprocket("sprocket", true),
    TakeShard("take_shard", false),
    StartDuel("start_duel",false),
    Tap("tap", false),
    Token("token", true),
    Untap("untap", true),
    WhiteBlueBlackLand("white_blue_black_land", false),
    WhiteBlueLand("white_blue_land", false),
    WhiteGreenBlueLand("white_green_blue_land", false),
    WhiteGreenLand("white_green_land", false),
    WhiteLand("white_land", false),
    WhiteRedBlackLand("white_red_black_land", false),
    WhiteRedLand("white_red_land", false),
    WinDuel("win_duel", false);

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
