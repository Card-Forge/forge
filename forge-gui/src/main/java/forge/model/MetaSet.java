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

package forge.model;

import com.google.common.base.Predicate;

import forge.card.IUnOpenedProduct;
import forge.card.UnOpenedProduct;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.limited.CustomLimited;
import forge.limited.SealedCardPoolGenerator;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;

import java.io.File;
import java.util.List;

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
        Full("F", "All cards"),
        Cube("C", "Cube"),
        JoinedSet("J", "Joined set"),
        Choose("Select", "Choose from list"),
        Combo("All", "Combined booster"),
        Random("Any", "Randomly selected"),
        Booster("B", "Booster"),
        SpecialBooster("S", "Special Booster"),
        Pack("T", "Tournament/Starter");
        
        private final String shortHand;
        public final String descriptiveName;
        private MetaSetType(String shortname, String descName) {
            shortHand = shortname;
            descriptiveName = descName;
        }

        public static MetaSetType smartValueOf(String trim) {
            for(MetaSetType mt : MetaSetType.values()) {
                if( mt.name().equalsIgnoreCase(trim) || mt.shortHand.equalsIgnoreCase(trim))
                    return mt;
            }
            throw new IllegalArgumentException(trim + " not recognized as Meta Set");
        }
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
        type = MetaSetType.smartValueOf(creationString.substring(0, idxFirstPar).trim());
        data = creationString.substring(idxFirstPar + 1, idxLastPar);
        String description = creationString.substring(idxLastPar + 1);
        code = description + "\u00A0(" + type.descriptiveName + ")"; // u00A0 (nbsp) will not be equal to simple space
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
                return new UnOpenedProduct(SealedProduct.Template.genericBooster);

            case Booster:
                return new UnOpenedProduct(FModel.getMagicDb().getBoosters().get(data));

            case SpecialBooster:
                return new UnOpenedProduct(FModel.getMagicDb().getSpecialBoosters().get(data));

            case Pack:
                return new UnOpenedProduct(FModel.getMagicDb().getTournamentPacks().get(data));

            case JoinedSet:
                Predicate<PaperCard> predicate = IPaperCard.Predicates.printedInSets(data.split(" "));
                return new UnOpenedProduct(SealedProduct.Template.genericBooster, predicate);

            case Choose: return UnOpenedMeta.choose(data);
            case Random: return UnOpenedMeta.random(data);
            case Combo:  return UnOpenedMeta.selectAll(data);

            case Cube:
                final File dFolder = new File(ForgeConstants.SEALED_DIR);

                if (!dFolder.exists()) {
                    throw new RuntimeException("GenerateSealed : folder not found -- folder is " + dFolder.getAbsolutePath());
                }

                if (!dFolder.isDirectory()) {
                    throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
                }

                List<String> dfData = FileUtil.readFile(ForgeConstants.SEALED_DIR + data + SealedCardPoolGenerator.FILE_EXT);
                final CustomLimited myCube = CustomLimited.parse(dfData, FModel.getDecks().getCubes());

                SealedProduct.Template fnPick = myCube.getSealedProductTemplate();
                return new UnOpenedProduct(fnPick, myCube.getCardPool());
                
            default: return null;
        }
    }
    
    @Override
    public String toString() {
        return code;
    } 

    public boolean isDraftable() {
        return draftable;
    }
}
