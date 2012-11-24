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
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;

import forge.Singletons;
import forge.game.limited.CustomLimited;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
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

    private final String type;
    private final String data;
    private final String code;
    // private BoosterGenerator boosterGen;

    /**
     * The constructor. A new MetaSet is currently only instantiated in CardBlock.java
     * when CardBlock information is read.
     * 
     * @param creationString
     *       a {@link java.lang.String} object.
     */
    public MetaSet(final String creationString) {


        String[] kv = new String [3];
        kv[0] = creationString.substring(0, creationString.indexOf('/'));
        kv[1] = creationString.substring(creationString.indexOf('/') + 1, creationString.lastIndexOf('/'));
        kv[2] = creationString.substring(creationString.lastIndexOf('/') + 1);
        // Display the parse results:
        // System.out.println("KV = '" + kv[0] + "', '" + kv[1] + "', '" + kv[2] + "'");

        type = kv[0];
        data = kv[1];

        if ("cube".equalsIgnoreCase(type)) {
            code = "*C:" + kv[2];
        }
        else if ("full".equalsIgnoreCase(type)) {
            code = "*FULL";
        }
        else if ("meta".equalsIgnoreCase(type)) {
            code = "*B:" + kv[2];
        }
        else if ("choose1".equalsIgnoreCase(type)) {
            code = "*!:" + kv[2];
        }
        else if ("random1".equalsIgnoreCase(type)) {
            code = "*?:" + kv[2];
        }
        else if ("combo".equalsIgnoreCase(type)) {
            code = "*+:" + kv[2];
        }
        else if ("booster".equalsIgnoreCase(type)) {
            code = "*" + kv[2];
        }
        else if ("pack".equalsIgnoreCase(type)) {
            code = "*" + kv[2] + "(S)";
        }
        else {
            code = null;
            throw new RuntimeException("Invalid MetaSet type: " + type);
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
     * Return the type.
     * 
     * @return
     *  String, type
     */
    public final String getType() {
        return type;
    }

    /**
     * 
     * Attempt to get a booster.
     * 
     * @return UnOpenedProduct, the generated booster.
     */
    public UnOpenedProduct getBooster() {

        ItemPool<CardPrinted> cardPool = null;

        if ("meta".equalsIgnoreCase(type) || "choose1".equalsIgnoreCase(type)
                || "random1".equalsIgnoreCase(type) || "combo".equalsIgnoreCase(type)) {
            cardPool = new ItemPool<CardPrinted>(CardPrinted.class);
        }

        if ("cube".equalsIgnoreCase(type)) {

            final File dFolder = new File("res/sealed/");

            if (!dFolder.exists()) {
                throw new RuntimeException("GenerateSealed : folder not found -- folder is "
                        + dFolder.getAbsolutePath());
            }

            if (!dFolder.isDirectory()) {
                throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
            }

            List<String> dfData = FileUtil.readFile("res/sealed/" + data + ".sealed");
            final CustomLimited myCube = CustomLimited.parse(dfData, Singletons.getModel().getDecks().getCubes());
            final BoosterGenerator bpCustom = new BoosterGenerator(myCube.getCardPool());
            final Function<BoosterGenerator, List<CardPrinted>> fnPick = new Function<BoosterGenerator, List<CardPrinted>>() {
              @Override
              public List<CardPrinted> apply(final BoosterGenerator pack) {
                   if (myCube.getIgnoreRarity()) {
                       if (!myCube.getSingleton()) {
                           return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, myCube.getNumCards(), 0);
                       } else {
                           return pack.getSingletonBoosterPack(myCube.getNumCards());
                       }
                   }
                   return pack.getBoosterPack(myCube.getNumbersByRarity(), 0, 0, 0);
               }

            };

            return new UnOpenedProduct(fnPick, bpCustom);
        }
        else if ("full".equalsIgnoreCase(type)) {
            final BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
            return new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpFull);
        }
        else if ("meta".equalsIgnoreCase(type)) {

            // NOTE: The following code is far from ideal in a number of ways. If someone can
            // think of a way to improve it, please do so. --BBU
            // ItemPool<CardPrinted> cardPool = new ItemPool<CardPrinted>(CardPrinted.class);
            for (CardPrinted aCard : CardDb.instance().getTraditionalCards()) {
                if (data.indexOf(aCard.getEdition()) > -1) {
                    cardPool.add(aCard);
                    // System.out.println("Added card" + aCard.getName());
                    }
            }

                final BoosterGenerator bpSets = new BoosterGenerator(cardPool);
                return new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpSets);
        } else if ("booster".equalsIgnoreCase(type)) {
            return new UnOpenedProduct(Singletons.getModel().getBoosters().get(data));
        } else if ("pack".equalsIgnoreCase(type)) {
            return new UnOpenedProduct(Singletons.getModel().getTournamentPacks().get(data));
        } else if ("choose1".equalsIgnoreCase(type)) {
            return new UnOpenedMeta(data, true);
        } else if ("random1".equalsIgnoreCase(type)) {
            return new UnOpenedMeta(data, false);
        } else if ("combo".equalsIgnoreCase(type)) {
            final BoosterGenerator bpSets = new BoosterGenerator(buildPool(data));
            return new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpSets);
        }
        else {
            throw new RuntimeException("Cannot initialize boosters for: " + type);
        }
    }

    /** 
     * Build a cardpool for the 'combo' special MetaSet type.
     * 
     * @param creationString
     *      the data that contains a collection of semicolon-separated metaset definitions
     * @return ItemPool<CardPrinted>
     *      the collection of cards
     *
     */
    private ItemPool<CardPrinted> buildPool(final String creationString) {

        ItemPool<CardPrinted> cardPool = new ItemPool<CardPrinted>(CardPrinted.class);

        List<MetaSet> metaSets = new ArrayList<MetaSet>();
        final String[] metas = creationString.split(";");

        for (int i = 0; i < metas.length; i++) {

            final String [] typeTest = metas[i].split("/");
            if (typeTest[0].equalsIgnoreCase("choose1") || typeTest[0].equalsIgnoreCase("random1")
                    || typeTest[0].equalsIgnoreCase("combo")) {
                        System.out.println("WARNING - MetaSet type '" + typeTest[0] + "' ignored in pool creation.");
                    }
            else if (typeTest[0].equalsIgnoreCase("full")) {
                for (CardPrinted aCard : CardDb.instance().getAllUniqueCards()) {
                    cardPool.add(aCard);
                }
                return cardPool;
            }
            final MetaSet addMeta = new MetaSet(metas[i]);
            metaSets.add(addMeta);
        }

        if (metaSets.size() < 1) {
            return null;
        }

        for (MetaSet mSet : metaSets) {
            if (mSet.type.equalsIgnoreCase("meta") || mSet.type.equalsIgnoreCase("booster")
                    || mSet.type.equalsIgnoreCase("pack")) {
                final String mData = new String(mSet.data);
                for (CardPrinted aCard : CardDb.instance().getTraditionalCards()) {
                    if (mData.indexOf(aCard.getEdition()) > -1) {
                        if (!cardPool.contains(aCard)) {
                            cardPool.add(aCard);
                            // System.out.println(mSet.type + " " + mData + ":  Added card: " + aCard.getName());
                            }
                        }
                }
            } else if (mSet.type.equalsIgnoreCase("cube")) {
                final File dFolder = new File("res/sealed/");

                if (!dFolder.exists()) {
                    throw new RuntimeException("GenerateSealed : folder not found -- folder is "
                            + dFolder.getAbsolutePath());
                }

                if (!dFolder.isDirectory()) {
                    throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
                }

                List<String> dfData = FileUtil.readFile("res/sealed/" + mSet.data + ".sealed");
                final CustomLimited myCube = CustomLimited.parse(dfData, Singletons.getModel().getDecks().getCubes());
                for (CardPrinted aCard : myCube.getCardPool().toFlatList()) {
                        if (!cardPool.contains(aCard)) {
                            cardPool.add(aCard);
                            // System.out.println(mSet.type + " " + mSet.data + ":  Added card: " + aCard.getName());
                            }
                }
            }
        }
        return cardPool;
    }
}
