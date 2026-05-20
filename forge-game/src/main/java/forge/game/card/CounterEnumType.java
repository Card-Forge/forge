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

package forge.game.card;

import java.util.Locale;

/**
 * The class Counters.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public enum CounterEnumType implements CounterType {

    M1M1("-1/-1", "-1/-1", 255, 110, 106, CounterAiCategory.Negative),
    P1P1("+1/+1", "+1/+1", 96, 226, 23, CounterAiCategory.Positive),

    LOYALTY("LOYAL", 198, 198, 198, CounterAiCategory.Positive),

    ACORN("ACORN", 139, 69, 19, CounterAiCategory.Positive),

    AEGIS("AEGIS", 207, 207, 207, CounterAiCategory.Positive),

    AGE("AGE", 255, 137, 57, CounterAiCategory.Negative),

    AIM("AIM", 255, 180, 0, CounterAiCategory.Positive),

    ARROW("ARROW", 237, 195, 0, CounterAiCategory.Positive),

    ARROWHEAD("ARWHD", 230, 191, 167, CounterAiCategory.Positive),

    AWAKENING("AWAKE", 0, 231, 79, CounterAiCategory.Neutral),

    BAIT("BAIT", 120, 100, 60, CounterAiCategory.Positive),

    BLAZE("BLAZE", 255, 124, 82, CounterAiCategory.Positive),

    BLESSING("BLESS", 251, 0, 94, CounterAiCategory.Positive),

    BLIGHT("BLGHT", 130, 115, 160, CounterAiCategory.Positive),

    BLOOD("BLOOD", 255, 108, 111, CounterAiCategory.Positive),

    BLOODLINE("BLDLN", 224, 44, 44, CounterAiCategory.Positive),

    BLOODSTAIN("BLDST", 224, 44, 44, CounterAiCategory.Positive),

    BORE("BORE", 98, 47, 34, CounterAiCategory.Positive),

    BOUNTY("BOUNT", 255, 158, 0, CounterAiCategory.Positive),

    BRAIN("BRAIN", 197, 62, 212, CounterAiCategory.Positive),

    BRIBERY("BRIBE", 172, 201, 235, CounterAiCategory.Negative),

    BRICK("BRICK", 226, 192, 164, CounterAiCategory.Positive),

    BURDEN("BURDEN", 135, 62, 35, CounterAiCategory.Positive),

    CAGE("CAGE", 155, 155, 155, CounterAiCategory.Positive),

    CARRION("CRRON", 255, 163, 222, CounterAiCategory.Positive),

    CELL ("CELL", 90, 10, 95, CounterAiCategory.Positive),

    CHARGE("CHARG", 246, 192, 0, CounterAiCategory.Positive),

    CHORUS("CHRUS", 0, 192, 246, CounterAiCategory.Positive),

    COIN("COIN", 255, 215, 0, CounterAiCategory.Positive),

    COLLECTION("CLLCT", 255, 215, 0, CounterAiCategory.Positive),

    COMPONENT("COMPN", 224, 160, 48, CounterAiCategory.Positive),

    CONQUEROR("CONQR", 225, 210, 25, CounterAiCategory.Positive),

    CONTESTED("CONTES", 255, 76, 2, CounterAiCategory.Positive),

    CORPSE("CRPSE", 230, 186, 209, CounterAiCategory.Positive),

    CORRUPTION("CRPTN", 210, 121, 210, CounterAiCategory.Positive),

    CROAK("CROAK", 155, 255, 5, CounterAiCategory.Positive),

    CREDIT("CRDIT", 188, 197, 234, CounterAiCategory.Positive),

    CRYSTAL("CRYST", 255, 85, 206, CounterAiCategory.Positive),

    CUBE("CUBE", 148, 219, 0, CounterAiCategory.Positive),

    CURRENCY("CURR", 223, 200, 0, CounterAiCategory.Positive),

    DEATH("DEATH", 255, 108, 110, CounterAiCategory.Positive),

    DEFENSE("DEF", 164, 23, 32, CounterAiCategory.Positive),

    DELAY("DELAY", 102, 206, 255, CounterAiCategory.Positive),

    DEPLETION("DPLT", 185, 201, 208, CounterAiCategory.Positive),

    DESCENT("DESCT", 175, 35, 40, CounterAiCategory.Positive),

    DESPAIR("DESPR", 238, 186, 187, CounterAiCategory.Positive),

    DEVOTION("DEVOT", 255, 111, 255, CounterAiCategory.Positive),

    DISCOVERY("DISCO", 12, 230, 100, CounterAiCategory.Positive),

    DIVINITY("DVNTY", 0, 233, 255, CounterAiCategory.Positive),

    DOOM("DOOM", 255, 104, 118, CounterAiCategory.Negative),

    DREAD ("DREAD", 205, 170, 240, CounterAiCategory.Positive),

    DREAM("DREAM", 190, 189, 255, CounterAiCategory.Positive),

    DUTY("DUTY", 232, 245, 245, CounterAiCategory.Positive),

    ECHO("ECHO", 225, 180, 255, CounterAiCategory.Positive),

    EGG("EGG", 255, 245, 195, CounterAiCategory.Positive),

    ELIXIR("ELIXR", 81, 221, 175, CounterAiCategory.Positive),

    EMBER("EMBER", 247, 52, 43, CounterAiCategory.Positive),

    EON("EON", 23, 194, 255, CounterAiCategory.Positive),

    ERUPTION("ERUPTION", 255, 124, 124, CounterAiCategory.Positive),

    EXPOSURE("EXPOSURE", 50, 180, 30, CounterAiCategory.Positive),

    EYEBALL("EYE", 184, 202, 201, CounterAiCategory.Positive),

    EYESTALK("EYES", 184, 202, 201, CounterAiCategory.Positive),

    EVERYTHING("EVRY", 255, 255, 255, CounterAiCategory.Positive),

    FADE("FADE", 159, 209, 192, CounterAiCategory.Positive),

    FATE("FATE", 255, 164, 226, CounterAiCategory.Positive),

    FEATHER("FTHR", 195, 202, 165, CounterAiCategory.Positive),

    FEEDING("FEED", 245, 21, 5, CounterAiCategory.Positive),

    FELLOWSHIP("FLWS", 255, 255, 255, CounterAiCategory.Positive),

    FETCH("FETCH", 180, 235, 52, CounterAiCategory.Positive),

    FILIBUSTER("FLBTR", 255, 179, 119, CounterAiCategory.Positive),

    FILM("FILM", 255, 255, 255, CounterAiCategory.Positive),

    FINALITY("FINAL", 255, 255, 255, CounterAiCategory.Negative),

    FIRE("FIRE", 240, 30, 35, CounterAiCategory.Positive),

    FLAME("FLAME", 255, 143, 43, CounterAiCategory.Positive),

    FLAVOR("FLAVOR", 208, 152, 97, CounterAiCategory.Positive), ///adventure only

    FLOOD("FLOOD", 0, 203, 255, CounterAiCategory.Positive),

    FORESHADOW("FRSHD", 144, 99, 207, CounterAiCategory.Positive),

    FUNGUS("FNGUS", 121, 219, 151, CounterAiCategory.Positive),

    FUNK("FUNK", 215, 24, 222, CounterAiCategory.Positive),

    FURY("FURY", 255, 120, 89, CounterAiCategory.Positive),

    FUSE("FUSE", 255, 122, 85, CounterAiCategory.Positive),

    GEM("GEM", 255, 99, 251, CounterAiCategory.Positive),

    GHOSTFORM("GHSTF", 223, 0, 254, CounterAiCategory.Neutral),

    GLYPH("GLYPH", 184, 202, 199, CounterAiCategory.Positive),

    GOLD("GOLD", 248, 191, 0, CounterAiCategory.Negative),

    GROWTH("GRWTH", 87, 226, 32, CounterAiCategory.Positive),

    HARMONY("HRMNY", 0, 230, 155, CounterAiCategory.Positive),

    HATCHING("HATCH", 204, 255, 204, CounterAiCategory.Positive),

    HATCHLING("HTCHL", 201, 199, 186, CounterAiCategory.Positive),

    HEALING("HEAL", 255, 166, 236, CounterAiCategory.Positive),

    HIT("HIT", 255, 245, 195, CounterAiCategory.Positive),

    HONE("HONE", 51, 227, 255, CounterAiCategory.Positive),

    HOPE("HOPE", 232, 245, 245, CounterAiCategory.Positive),

    HOOFPRINT("HOOF", 233, 189, 170, CounterAiCategory.Positive),

    HOUR("HOUR", 198, 197, 210, CounterAiCategory.Positive),

    HOURGLASS("HRGLS", 0, 215, 255, CounterAiCategory.Positive),

    HUNGER("HUNGR", 255, 91, 149, CounterAiCategory.Positive),

    HUSK("HUSK", 227, 212, 173, CounterAiCategory.Positive),

    ICE("ICE", 0, 239, 255, CounterAiCategory.Positive),

    IMPOSTOR("IMPO", 173, 194, 255, CounterAiCategory.Positive),

    INCARNATION("INCRN", 247, 206, 64, CounterAiCategory.Negative),

    INCUBATION("INCBT", 40, 210, 25, CounterAiCategory.Positive),

    INGREDIENT("INGRD", 180, 50, 145, CounterAiCategory.Positive),

    INFECTION("INFCT", 0, 230, 66, CounterAiCategory.Positive),

    INFLUENCE("INFL", 201, 99, 212, CounterAiCategory.Positive),

    INGENUITY("INGTY", 67, 186, 205, CounterAiCategory.Positive),

    INTEL("INTEL", 80, 250, 180, CounterAiCategory.Positive),

    INTERVENTION("INTRV", 205, 203, 105, CounterAiCategory.Positive),

    INVITATION("INVIT", 205, 0, 26, CounterAiCategory.Positive),

    ISOLATION("ISOLT", 250, 190, 0, CounterAiCategory.Positive),

    JAVELIN("JAVLN", 180, 206, 172, CounterAiCategory.Positive),

    JUDGMENT("JUDGM", 249, 220, 52, CounterAiCategory.Positive),

    KI("KI", 190, 189, 255, CounterAiCategory.Positive),

    KICK("KICK", 255, 255, 240, CounterAiCategory.Positive),

    KNOWLEDGE("KNOWL", 0, 115, 255, CounterAiCategory.Positive),

    LANDMARK("LNMRK", 186, 28, 28, CounterAiCategory.Positive),

    LEVEL("LEVEL", 60, 222, 185, CounterAiCategory.Positive),

    LOOT("LOOT", 255, 215, 0, CounterAiCategory.Positive),

    LORE("LORE", 209, 198, 161, CounterAiCategory.Positive),

    LUCK("LUCK", 185, 174, 255, CounterAiCategory.Positive),

    MANABOND("MANA", 0, 255, 0, CounterAiCategory.Positive),

    M0M1("-0/-1", "-0/-1", 255, 110, 106, CounterAiCategory.Negative),

    M0M2("-0/-2", "-0/-2", 255, 110, 106, CounterAiCategory.Negative),

    M1M0("-1/-0", "-1/-0", 255, 110, 106, CounterAiCategory.Negative),

    M2M1("-2/-1", "-2/-1", 255, 110, 106, CounterAiCategory.Negative),

    M2M2("-2/-2", "-2/-2", 255, 110, 106, CounterAiCategory.Negative),

    MAGNET("MAGNT", 198, 197, 210, CounterAiCategory.Positive),

    MANA("MANA", 0, 237, 152, CounterAiCategory.Positive),

    MANIFESTATION("MNFST", 104, 225, 8, CounterAiCategory.Neutral),

    MANNEQUIN("MANQN", 206, 199, 162, CounterAiCategory.Positive),

    MATRIX("MATRX", 183, 174, 255, CounterAiCategory.Positive),

    MEMORY("MEMRY", 174, 183, 255, CounterAiCategory.Positive),

    MIDWAY("MDWAY", 84, 101, 207, CounterAiCategory.Positive),

    MINE("MINE", 255, 100, 127, CounterAiCategory.Positive),

    MINING("MINNG", 184, 201, 207, CounterAiCategory.Positive),

    MIRE("MIRE", 153, 209, 199, CounterAiCategory.Positive),

    MUSIC("MUSIC", 255, 138, 255, CounterAiCategory.Negative),

    MUSTER("MUSTR", 235, 196, 0, CounterAiCategory.Positive),

    NECRODERMIS("NECRO", 80, 209, 250, CounterAiCategory.Positive),

    NET("NET", 0, 221, 251, CounterAiCategory.Positive),

    NEST("NEST", 80, 80, 50, CounterAiCategory.Positive),

    OIL("OIL", 99, 102, 106, CounterAiCategory.Positive),

    OMEN("OMEN", 255, 178, 120, CounterAiCategory.Positive),

    ORE("ORE", 200, 201, 163, CounterAiCategory.Positive),

    PAGE("PAGE", 218, 195, 162, CounterAiCategory.Positive),

    PAIN("PAIN", 255, 108, 111, CounterAiCategory.Positive),

    PALLIATION("PALLI", 114, 243, 250, CounterAiCategory.Positive),

    PARALYZATION("PRLYZ", 220, 201, 0, CounterAiCategory.Negative),

    PETAL("PETAL", 255, 162, 216, CounterAiCategory.Positive),

    PETRIFICATION("PETRI", 185, 201, 208, CounterAiCategory.Neutral),

    PIN("PIN", 194, 196, 233, CounterAiCategory.Positive),

    PLAGUE("PLGUE", 94, 226, 25, CounterAiCategory.Positive),

    PLOT("PLOT", 255, 172, 133, CounterAiCategory.Positive),

    PRESSURE("PRESS", 255, 164, 159, CounterAiCategory.Positive),

    PHYLACTERY("PHYLA", 117, 219, 153, CounterAiCategory.Positive),

    PHYRESIS("PHYRE", 125, 97, 128, CounterAiCategory.Positive),

    PLAN("PLAN", 20, 35, 235, CounterAiCategory.Positive),

    POINT("POINT", 153, 255, 130, CounterAiCategory.Positive),

    POLYP("POLYP", 236, 185, 198, CounterAiCategory.Positive),

    POSSESSION("POSSN", 60, 65, 85, CounterAiCategory.Positive),

    PREY("PREY", 240, 0, 0, CounterAiCategory.Positive),

    PUPA("PUPA", 0, 223, 203, CounterAiCategory.Negative),

    P0P1("+0/+1", "+0/+1", 96, 226, 23, CounterAiCategory.Positive),

    P0P2("+0/+2", "+0/+2", 96, 226, 23, CounterAiCategory.Positive),

    P1P0("+1/+0", "+1/+0", 96, 226, 23, CounterAiCategory.Positive),

    P1P2("+1/+2", "+1/+2", 96, 226, 23, CounterAiCategory.Positive),

    P2P0("+2/+0", "+2/+0", 96, 226, 23, CounterAiCategory.Positive),

    P2P2("+2/+2", "+2/+2", 96, 226, 23, CounterAiCategory.Positive),

    QUEST("QUEST", 251, 189, 0, CounterAiCategory.Neutral),

    RALLY("RALLY", 25, 230, 225, CounterAiCategory.Positive),

    RELEASE("RELEASE", 200, 210, 50, CounterAiCategory.Positive),

    REPRIEVE("REPR", 240, 120, 50, CounterAiCategory.Positive),

    REJECTION("REJECT", 212, 235, 242, CounterAiCategory.Positive),

    REV("REV", 255, 108, 111, CounterAiCategory.Positive),

    REVIVAL("REVIVL", 130, 230, 50, CounterAiCategory.Positive),

    RIBBON("RIBBON", 233, 245, 232, CounterAiCategory.Positive),

    RITUAL("RITUAL", 155, 17, 30, CounterAiCategory.Positive),

    ROPE("ROPE", 239, 223, 187, CounterAiCategory.Positive),

    RUST("RUST", 255, 181, 116, CounterAiCategory.Negative),

    SCREAM("SCREM", 0, 220, 255, CounterAiCategory.Positive),

    SCROLL("SCRLL", 206, 199, 162, CounterAiCategory.Positive),

    SHELL("SHELL", 190, 207, 111, CounterAiCategory.Negative),

    SHIELD("SHLD", 202, 198, 186, CounterAiCategory.Positive),

    SHRED("SHRED", 255, 165, 152, CounterAiCategory.Positive),

    SILVER("SILVER", 192, 192, 192, CounterAiCategory.Positive),

    SKEWER("SKEWER", 202, 192, 156, CounterAiCategory.Positive),

    SLEEP("SLEEP", 178, 192, 255, CounterAiCategory.Negative),

    SLUMBER("SLMBR", 178, 205, 255, CounterAiCategory.Negative),

    SLEIGHT("SLGHT", 185, 174, 255, CounterAiCategory.Negative),

    SLIME("SLIME", 101, 220, 163, CounterAiCategory.Positive),

    SOUL("SOUL", 243, 190, 247, CounterAiCategory.Positive),

    SOOT("SOOT", 211, 194, 198, CounterAiCategory.Positive),

    SPITE("SPITE", 0, 218, 255, CounterAiCategory.Positive),

    SPORE("SPORE", 122, 218, 150, CounterAiCategory.Positive),

    STASH("STASH", 248, 191, 0, CounterAiCategory.Positive),

    STORAGE("STORG", 255, 177, 121, CounterAiCategory.Positive),

    STORY("STORY", 180, 72, 195, CounterAiCategory.Positive),

    STRIFE("STRFE", 255, 89, 223, CounterAiCategory.Positive),

    STUDY("STUDY", 226, 192, 165, CounterAiCategory.Positive),

    STUN("STUN", 226, 192, 165, CounterAiCategory.Negative),

    SUPPLY("SPPLY", 70, 105, 60, CounterAiCategory.Positive),

    TAKEOVER("TKVR", 63, 49, 191, CounterAiCategory.Positive),

    TASK("TASK", 191, 63, 49, CounterAiCategory.Positive),

    THEFT("THEFT", 255, 176, 125, CounterAiCategory.Positive),

    TIDE("TIDE", 0, 212, 187, CounterAiCategory.Positive),

    TIME("TIME", 255, 121, 255, CounterAiCategory.Positive),

    TOWER("tower", "TOWER", 0, 239, 255, CounterAiCategory.Positive),

    TRAINING("TRAIN", 220, 201, 0, CounterAiCategory.Neutral),

    TRAP("TRAP", 255, 121, 86, CounterAiCategory.Positive),

    TREASURE("TRSUR", 255, 184, 0, CounterAiCategory.Positive),

    UNITY("UNITY", 242, 156, 255, CounterAiCategory.Positive),

    UNLOCK("UNLCK", 222, 146, 205, CounterAiCategory.Positive),

    VALOR("VALOR", 252, 250, 222, CounterAiCategory.Positive),

    VELOCITY("VELO", 255, 95, 138, CounterAiCategory.Positive),

    VERSE("VERSE", 0, 237, 155, CounterAiCategory.Positive),

    VITALITY("VITAL", 255, 94, 142, CounterAiCategory.Positive),

    VORTEX("VORTX", 142, 200, 255, CounterAiCategory.Positive),

    VOYAGE("VOYAGE", 38, 150, 137, CounterAiCategory.Positive),

    WAGE("WAGE", 242, 190, 106, CounterAiCategory.Negative),

    WINCH("WINCH", 208, 195, 203, CounterAiCategory.Positive),

    WIND("WIND", 0, 236, 255, CounterAiCategory.Positive),

    WISH("WISH", 255, 85, 206, CounterAiCategory.Positive),

    WRECK("WRECK", 208, 55, 255, CounterAiCategory.Positive),

    // Player Counters

    ENERGY("ENRGY", CounterAiCategory.Positive),

    EXPERIENCE("EXP", CounterAiCategory.Positive),

    POISON("POISN", CounterAiCategory.Negative),

    RAD("RAD", CounterAiCategory.Neutral),

    TICKET("TICKET", CounterAiCategory.Positive),

    ;

    private String name, counterOnCardDisplayName;
    private int red, green, blue;
    private CounterAiCategory aiCategory;

    CounterEnumType(final String counterOnCardDisplayName, CounterAiCategory aiCategory) {
        this(counterOnCardDisplayName, 255, 255, 255, aiCategory);
    }

    CounterEnumType(final String counterOnCardDisplayName, final int red, final int green, final int blue, CounterAiCategory aiCategory) {
        this.name = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
        this.counterOnCardDisplayName = counterOnCardDisplayName;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.aiCategory = aiCategory;
    }

    CounterEnumType(final String name, final String counterOnCardDisplayName, final int red, final int green, final int blue, CounterAiCategory aiCategory) {
        this(counterOnCardDisplayName, red, green, blue, aiCategory);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }

    @Override
    public String getCounterOnCardDisplayName() {
        return counterOnCardDisplayName;
    }

    public static CounterEnumType getType(final String name) {
        final String replacedName = name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase(Locale.ROOT);
        return Enum.valueOf(CounterEnumType.class, replacedName);
    }

    @Override
    public boolean is(CounterEnumType eType) {
        return this == eType;
    }
}
