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

import com.google.common.collect.ImmutableList;

/**
 * The class Counters.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public enum CounterEnumType {

    M1M1("-1/-1", "-1/-1", 255, 110, 106),
    P1P1("+1/+1", "+1/+1", 96, 226, 23),

    LOYALTY("LOYAL", 198, 198, 198),

    ACORN("ACORN", 139,69,19),

    AEGIS("AEGIS", 207, 207, 207),

    AGE("AGE", 255, 137, 57),

    AIM("AIM", 255, 180, 0),

    ARROW("ARROW", 237, 195, 0),

    ARROWHEAD("ARWHD", 230, 191, 167),

    AWAKENING("AWAKE", 0, 231, 79),

    BLAZE("BLAZE", 255, 124, 82),
    
    BLESSING("BLESS", 251, 0, 94),

    BLOOD("BLOOD", 255, 108, 111),

    BLOODLINE("BLDLN", 224, 44, 44),

    BOUNTY("BOUNT", 255, 158, 0),

    BRIBERY("BRIBE", 172, 201, 235),

    BRICK("BRICK", 226, 192, 164),

    BURDEN("BURDEN", 135, 62, 35),

    CAGE("CAGE", 155, 155, 155),

    CARRION("CRRON", 255, 163, 222),

    CHARGE("CHARG", 246, 192, 0),

    COIN("COIN",255,215,0),

    COLLECTION("CLLCT", 255, 215, 0),

    COMPONENT("COMPN", 224, 160, 48),

    CONTESTED("CONTES", 255, 76, 2),

    CORPSE("CRPSE", 230, 186, 209),

    CORRUPTION("CRPTN", 210, 121, 210),

    CROAK("CROAK", 155, 255, 5),

    CREDIT("CRDIT", 188, 197, 234),

    CRYSTAL("CRYST", 255, 85, 206),

    CUBE("CUBE", 148, 219, 0),

    CURRENCY("CURR", 223, 200, 0),

    DEATH("DEATH", 255, 108, 110),

    DEFENSE("DEF", 164, 23, 32),

    DELAY("DELAY", 102, 206, 255),

    DEPLETION("DPLT", 185, 201, 208),

    DESCENT("DESCT", 175, 35, 40),

    DESPAIR("DESPR", 238, 186, 187),

    DEVOTION("DEVOT", 255, 111, 255),

    DIVINITY("DVNTY", 0, 233, 255),

    DOOM("DOOM", 255, 104, 118),

    DREAM("DREAM", 190, 189, 255),

    ECHO("ECHO", 225, 180, 255),

    EGG("EGG", 255, 245, 195),

    ELIXIR("ELIXR", 81, 221, 175),

    EON("EON", 23, 194, 255),

    EMBER("EMBER", 247, 52, 43),

    EYEBALL("EYE", 184, 202, 201),

    EYESTALK("EYES", 184, 202, 201),

    FADE("FADE", 159, 209, 192),

    FATE("FATE", 255, 164, 226),

    FEATHER("FTHR", 195, 202, 165),

    FETCH("FETCH", 180, 235, 52),

    FILIBUSTER("FLBTR", 255, 179, 119),

    FLAME("FLAME", 255, 143, 43),
    
    FLAVOR("FLAVOR", 208, 152, 97), ///adventure only

    FLOOD("FLOOD", 0, 203, 255),

    FORESHADOW("FRSHD",144,99, 207),

    FUNGUS("FNGUS", 121, 219, 151),

    FUNK("FUNK", 215, 24, 222),

    FURY("FURY", 255, 120, 89),

    FUSE("FUSE", 255, 122, 85),

    GEM("GEM", 255, 99, 251),

    GHOSTFORM("GHSTF", 223, 0, 254),

    GLYPH("GLYPH", 184, 202, 199),

    GOLD("GOLD", 248, 191, 0),

    GROWTH("GRWTH", 87, 226, 32),

    HARMONY("HRMNY", 0, 230, 155),

    HATCHING("HATCH", 204, 255, 204),

    HATCHLING("HTCHL", 201, 199, 186),

    HEALING("HEAL", 255, 166, 236),

    HIT("HIT", 255, 245, 195),

    HONE("HONE", 51, 227, 255),
    
    HOPE("HOPE", 232, 245, 245),

    HOOFPRINT("HOOF", 233, 189, 170),

    HOUR("HOUR", 198, 197, 210),

    HOURGLASS("HRGLS", 0, 215, 255),

    HUNGER("HUNGR", 255, 91, 149),

    HUSK("HUSK", 227, 212, 173),

    ICE("ICE", 0, 239, 255),

    INCARNATION("INCRN", 247, 206, 64),

    INFECTION("INFCT", 0, 230, 66),
    
    INFLUENCE("INFL", 201, 99, 212),

    INGENUITY("INGTY", 67, 186, 205),

    INTEL("INTEL", 80, 250, 180),

    INTERVENTION("INTRV", 205, 203, 105),

    INVITATION("INVIT", 205, 0, 26),

    ISOLATION("ISOLT", 250, 190, 0),

    JAVELIN("JAVLN", 180, 206, 172),

    JUDGMENT("JUDGM", 249, 220, 52),

    KI("KI", 190, 189, 255),

    KICK("KICK", 255, 255, 240),

    KNOWLEDGE("KNOWL", 0, 115, 255),

    LANDMARK("LNMRK", 186, 28, 28),

    LEVEL("LEVEL", 60, 222, 185),

    LORE("LORE", 209, 198, 161),

    LUCK("LUCK", 185, 174, 255),

    MANABOND("MANA", 0, 255, 0),

    M0M1("-0/-1", "-0/-1", 255, 110, 106),

    M0M2("-0/-2", "-0/-2", 255, 110, 106),

    M1M0("-1/-0", "-1/-0", 255, 110, 106),

    M2M1("-2/-1", "-2/-1", 255, 110, 106),

    M2M2("-2/-2", "-2/-2", 255, 110, 106),

    MAGNET("MAGNT", 198, 197, 210),

    MANA("MANA", 0, 237, 152),

    MANIFESTATION("MNFST", 104, 225, 8),

    MANNEQUIN("MANQN", 206, 199, 162),

    MATRIX("MATRX", 183, 174, 255),

    MINE("MINE", 255, 100, 127),

    MINING("MINNG", 184, 201, 207),

    MIRE("MIRE", 153, 209, 199),

    MUSIC("MUSIC", 255, 138, 255),

    MUSTER("MUSTR", 235, 196, 0),

    NECRODERMIS("NECRO", 80, 209, 250),

    NET("NET", 0, 221, 251),

    OIL("OIL", 99, 102, 106),

    OMEN("OMEN", 255, 178, 120),

    ORE("ORE", 200, 201, 163),

    PAGE("PAGE", 218, 195, 162),

    PAIN("PAIN", 255, 108, 111),

    PARALYZATION("PRLYZ", 220, 201, 0),

    PETAL("PETAL", 255, 162, 216),

    PETRIFICATION("PETRI", 185, 201, 208),

    PIN("PIN", 194, 196, 233),

    PLAGUE("PLGUE", 94, 226, 25),

    PLOT("PLOT", 255, 172, 133),

    PRESSURE("PRESS", 255, 164, 159),

    PHYLACTERY("PHYLA", 117, 219, 153),

    PHYRESIS("PHYRE", 125, 97, 128),

    POINT("POINT", 153, 255, 130),

    POLYP("POLYP", 236, 185, 198),

    PREY("PREY", 240, 0, 0),

    PUPA("PUPA", 0, 223, 203),

    P0P1("+0/+1", "+0/+1", 96, 226, 23),

    P0P2("+0/+2", "+0/+2", 96, 226, 23),

    P1P0("+1/+0", "+1/+0", 96, 226, 23),

    P1P2("+1/+2", "+1/+2", 96, 226, 23),

    P2P0("+2/+0", "+2/+0", 96, 226, 23),

    P2P2("+2/+2", "+2/+2", 96, 226, 23),

    QUEST("QUEST", 251, 189, 0),

    REPRIEVE("REPR", 240, 120, 50),

    REJECTION("REJECT", 212, 235, 242),
    
    RIBBON("RIBBON", 233, 245, 232),

    RITUAL("RITUAL", 155, 17, 30),

    ROPE("ROPE", 239, 223, 187),

    RUST("RUST", 255, 181, 116),

    SCREAM("SCREM", 0, 220, 255),

    SCROLL("SCRLL", 206, 199, 162),

    SHELL("SHELL", 190, 207, 111),

    SHIELD("SHLD", 202, 198, 186),

    SHRED("SHRED", 255, 165, 152),

    SILVER("SILVER", 192, 192, 192),
    
    SKEWER("SKEWER", 202, 192, 156),

    SLEEP("SLEEP", 178, 192, 255),

    SLUMBER("SLMBR", 178, 205, 255),

    SLEIGHT("SLGHT", 185, 174, 255),

    SLIME("SLIME", 101, 220, 163),

    SOUL("SOUL", 243, 190, 247),

    SOOT("SOOT", 211, 194, 198),

    SPITE("SPITE", 0, 218, 255),

    SPORE("SPORE", 122, 218, 150),

    STASH("STASH", 248, 191, 0),

    STORAGE("STORG", 255, 177, 121),

    STORY("STORY", 180, 72, 195),

    STRIFE("STRFE", 255, 89, 223),

    STUDY("STUDY", 226, 192, 165),

    STUN("STUN", 226, 192, 165),

    TASK("TASK", 191, 63, 49),

    THEFT("THEFT", 255, 176, 125),

    TIDE("TIDE", 0, 212, 187),

    TIME("TIME", 255, 121, 255),

    TOWER("tower", "TOWER", 0, 239, 255),

    TRAINING("TRAIN", 220, 201, 0),

    TRAP("TRAP", 255, 121, 86),

    TREASURE("TRSUR", 255, 184, 0),

    UNITY("UNITY", 242, 156, 255),

    VALOR("VALOR", 252, 250, 222),

    VELOCITY("VELO", 255, 95, 138),

    VERSE("VERSE", 0, 237, 155),

    VITALITY("VITAL", 255, 94, 142),

    VORTEX("VORTX", 142, 200, 255),

    VOYAGE("VOYAGE", 38, 150, 137),

    WAGE("WAGE", 242, 190, 106),

    WINCH("WINCH", 208, 195, 203),

    WIND("WIND", 0, 236, 255),

    WISH("WISH", 255, 85, 206),

    // Player Counters

    ENERGY("ENRGY"),

    EXPERIENCE("EXP"),

    POISON("POISN"),

    TICKET("TICKET"),

    // Keyword Counters
/*
    FLYING("Flying"),
    FIRSTSTRIKE("First Strike"),
    DOUBLESTRIKE("Double Strike"),
    DEATHTOUCH("Deathtouch"),
    HEXPROOF("Hexproof"),
    INDESTRUCTIBLE("Indestructible"),
    LIFELINK("Lifelink"),
    MENACE("Menace"),
    REACH("Reach"),
    TRAMPLE("Trample"),
    VIGILANCE("Vigilance")
    SHADOW("Shadow")
//*/
    ;

    private String name, counterOnCardDisplayName;
    private int red, green, blue;

    CounterEnumType() {
        this.name = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
        if (red == 0 && green == 0 && blue == 0) {
            red = 255;
            green = 255;
            blue = 255;
        }
    }

    CounterEnumType(final String counterOnCardDisplayName) {
        this();
        this.counterOnCardDisplayName = counterOnCardDisplayName;
    }

    CounterEnumType(final String counterOnCardDisplayName, final int red, final int green, final int blue) {
        this(counterOnCardDisplayName);
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    CounterEnumType(final String name, final String counterOnCardDisplayName, final int red, final int green, final int blue) {
        this(counterOnCardDisplayName, red, green, blue);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public String getCounterOnCardDisplayName() {
        return counterOnCardDisplayName;
    }

    public static CounterEnumType getType(final String name) {
        final String replacedName = name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase(Locale.ROOT);
        return Enum.valueOf(CounterEnumType.class, replacedName);
    }

    public static final ImmutableList<CounterEnumType> values = ImmutableList.copyOf(values());

}
