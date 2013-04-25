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

package forge.card;

import java.io.File;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.game.limited.CustomLimited;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.util.FileUtil;

/** 
 * The class MetaSet. This class is used to define 'special'
 * sets within a (fantasy) block, like custom sets (cubes),
 * combination sets (sub-blocks), and full cardpool.
 * 
 * NOTE: The format of a MetaSet definition is as follows
 * (in a blocks definition file):
 * 
 *      "metaX A/B/C"
 * 
 * where "X" is an integer from 0...8 (just like for sets)
 * 
 * "A" is either "cube", "meta", "full", "choose1", "random1",
 * "set", "pack", or "combo".
 * 
 * "full" uses all available cards for this booster, just like
 * the full cardpool option. The values of "B" and "C" are not
 * relevant ("B" is not used at all, and the displayed name is
 * always "*FULL").
 * 
 *  "meta" uses a cardpool that is combined from several
 *  editions. The parameter "B" is a list of 3-letter
 *  edition codes, separated with commas, e.g.
 *  "2ED,ARN,ATQ". The parameter "C" is the name
 *  that is displayed for this meta-booster in the
 *  set selection menu.
 * 
 *  "cube" uses a previously defined custom sealed deck
 *  deck cube as the cardpool for this booster. The cube
 *  definition file must be in res/sealed/ and have a
 *  ".sealed" extension. The related .dck file must be
 *  in the res/decks/cube directory.
 *  "B" is the name of the cube definition file without
 *  the ".sealed" extension, e.g. "juzamjedi".
 *  "C" is the name that is displayed for this meta-booster
 *  in the set selection menu.
 *
 *  The new types added after beta 1.2.14:
 *
 *  "choose1": define several metasets in a semicolon-separated (;)
 *  list in value B, the player will choose one of them.
 *
 *  "random1": like choose1, except that the player will get a
 *  random pack.
 *
 *  "combo": define several metasets in a semicolon-separated (;)
 *  list in value B, the booster will be based on the combined
 *  cardpool of all these. Note that if one of the metasets is
 *  a "full", the rest are irrelevant.
 *
 *  "booster": generate a single booster based on value B set. (You
 *  should use this only for combo, choose1 and random1 metasets,
 *  otherwise use normal Sets instead of MetaSets in the block
 *  definition!)
 *
 *  "pack": like set, but attempts to generate a starter pack instead
 *  of a booster. If starter packs are not available for value B set,
 *  a booster is generated instead.
 *
 */
public class MetaSet {
    
    private enum MetaSetType { 
        Full,
        Cube,
        JoinedSet,
        Choose,
        Random,
        Booster,
        Pack
    }

    private final MetaSetType type;
    private final String data;
    private final String code;
    private final boolean draftable; 
    // private BoosterGenerator boosterGen;

    /**
     * The constructor. A new MetaSet is currently only instantiated in CardBlock.java
     * when CardBlock information is read.
     * 
     * @param creationString
     *       a {@link java.lang.String} object.
     */
    public MetaSet(final String creationString, boolean canDraft) {
        int idxFirstPar = creationString.indexOf('(');
        int idxLastPar = creationString.lastIndexOf(')');

        draftable = canDraft; 
        type = MetaSetType.valueOf(creationString.substring(0, idxFirstPar).trim());
        data = creationString.substring(idxFirstPar + 1, idxLastPar);
        String description = creationString.substring(idxLastPar + 1);

        switch (type) {
            case Cube: code = "*C:" + description; break;
            case Full: code = "*FULL"; break;
            case JoinedSet: code = "*B:" + description; break;
            case Choose: code = "*!:" + description; break;
            case Random: code = "*?:" + description; break;
            case Booster: code = "*" + description; break;
            case Pack: code = "*" + description + "(S)"; break; 

            default: throw new RuntimeException("Invalid MetaSet type: " + type); 
        }
    }

    /**
     * Return the code.
     * 
     * @return
     *  String, code
     */
    public final String getCode() {
        return code;
    }

    /**
     * 
     * Attempt to get a booster.
     * 
     * @return UnOpenedProduct, the generated booster.
     */
    public IUnOpenedProduct getBooster() {

        switch(type) {
            case Full:
                return new UnOpenedProduct(BoosterTemplate.genericBooster);

            case Booster:
                return new UnOpenedProduct(Singletons.getModel().getBoosters().get(data));

            case Pack:
                return new UnOpenedProduct(Singletons.getModel().getTournamentPacks().get(data));

            case JoinedSet:
                Predicate<CardPrinted> predicate = IPaperCard.Predicates.printedInSets(data.split(" "));
                Iterable<CardPrinted> pool = Iterables.filter(CardDb.instance().getAllCards(), predicate); 
                return new UnOpenedProduct(BoosterTemplate.genericBooster, pool);

            case Choose:
                return new UnOpenedMeta(data, true);

            case Random:
                return new UnOpenedMeta(data, false);

            case Cube:
                final File dFolder = new File("res/sealed/");

                if (!dFolder.exists()) {
                    throw new RuntimeException("GenerateSealed : folder not found -- folder is " + dFolder.getAbsolutePath());
                }

                if (!dFolder.isDirectory()) {
                    throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
                }

                List<String> dfData = FileUtil.readFile("res/sealed/" + data + ".sealed");
                final CustomLimited myCube = CustomLimited.parse(dfData, Singletons.getModel().getDecks().getCubes());

                SealedProductTemplate fnPick = myCube.getSealedProductTemplate();
                return new UnOpenedProduct(fnPick, myCube.getCardPool());
                
            default: return null;
        }
    }

    public boolean isDraftable() {
        return draftable;
    }
}
