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
package forge.game.limited;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.google.common.base.Function;

import forge.Singletons;
import forge.card.BoosterGenerator;
import forge.card.CardBlock;
import forge.card.CardEdition;
import forge.card.UnOpenedProduct;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.util.FileUtil;

/**
 * <p>
 * SealedDeckFormat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SealedDeckFormat {
    private final ArrayList<UnOpenedProduct> product = new ArrayList<UnOpenedProduct>();
    private List<String> partiality;

    /** The Land set code. */
    private String[] landSetCode = { "" };

    /**
     * <p>
     * Constructor for SealedDeck.
     * </p>
     * 
     * @param sealedType
     *            a {@link java.lang.String} object.
     */
    public SealedDeckFormat(final String sealedType) {

        partiality = new ArrayList<String>();

        if (sealedType.equals("Full")) {

            final BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());

            // Choose number of boosters
            final Integer[] integers = new Integer[10];

            for (int i = 0; i < 10; i++) {
                integers[i] = Integer.valueOf(i + 3);
            }

            Integer nrBoosters = GuiUtils.chooseOne("How many booster packs?", integers);

            for (int i = 0; i < nrBoosters; i++) {
                this.product.add(new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpFull));
            }

            this.getLandSetCode()[0] = CardDb.instance().getCard("Plains").getEdition();
        } else if (sealedType.equals("Block") || sealedType.equals("FBlock")) {

            List<CardBlock> blocks = new ArrayList<CardBlock>();

            if (sealedType.equals("Block")) {
                for (CardBlock b : Singletons.getModel().getBlocks()) {
                    blocks.add(b);
                }
            }
            else {
                for (CardBlock b : Singletons.getModel().getFantasyBlocks()) {
                    blocks.add(b);
                }
            }

            final CardBlock block = GuiUtils.chooseOne("Choose Block", blocks);

            final CardEdition[] cardSets = block.getSets();
            final String[] sets = new String[cardSets.length + block.getNumberMetaSets()];
            for (int k = cardSets.length - 1; k >= 0; --k) {
                sets[k] = cardSets[k].getCode();
            }

            final int nPacks = block.getCntBoostersSealed();

            if (block.getNumberMetaSets() > 0) {

                int j = cardSets.length;

                for (int k = 0; k < block.getNumberMetaSets(); k++) {
                    sets[j + k] = block.getMetaSet(k).getCode();
                }
            }

            final List<String> setCombos = getSetCombos(sets, nPacks);

            while (setCombos == null) {
                throw new RuntimeException("Unsupported amount of packs (" + nPacks + ") in a Sealed Deck block!");
            }

            if (sets.length > 1) {
                final Object p = GuiUtils.chooseOne("Choose Set Combination", setCombos);

                final String[] pp = p.toString().split("/");

                // Consider up to two starter packs --BBU
                boolean starter1 = false;
                boolean starter2 = false;
                int starter1idx = -1;
                int starter2idx = -2;

                for (int j = nPacks - 1; j >= 0 && !starter2; j--) {

                    if (Singletons.getModel().getTournamentPacks().contains(pp[j])) {
                        if (starter1) {
                            starter2 = true;
                            starter2idx = j;

                            // Prefer a different second set
                            if (j > 0 && pp[starter1idx].equals(pp[starter2idx])) {
                                for (int k = j; k >= 0; k--) {
                                    if (Singletons.getModel().getTournamentPacks().contains(pp[k])
                                            && !(pp[k].equals(pp[j]))) {
                                                starter2idx = k;
                                                break; // Found, don't look any further.
                                                }
                                        }
                                    }
                                }
                            else {
                                starter1 = true;
                                starter1idx = j;
                                }
                        }
                    }

                if (starter1 || starter2) {
                    final List<String> starterPacks = new ArrayList<String>();

                    // The option to use booster packs only, no starter packs
                    starterPacks.add("(None)");

                    // Add option for the first starter pack
                    if (starter1) {
                        starterPacks.add(String.format("%s", pp[starter1idx]));
                    }

                    // Add a separate option for the second starter pack if different from the first
                    if (starter2 && !(pp[starter2idx].equals(pp[starter1idx]))) {
                        starterPacks.add(String.format("%s", pp[starter2idx]));
                    }

                    // If both can have starter packs, add option for both
                    if (starter1 && starter2) {
                        starterPacks.add(String.format("Two packs (%s, %s)", pp[starter1idx], pp[starter2idx]));
                    }

                    final Object starterResult = GuiUtils.chooseOne("Choose starter pack(s):", starterPacks);

                    // Analyze the choice
                    final String starters = starterResult.toString();

                    if (starters.equals("(None)")) {
                        starter1 = false;
                        starter2 = false;
                    }
                    else if (starters.equals(pp[starter1idx])) {
                        starter1 = true;
                        starter2 = false;
                    }
                    else if (starters.equals(pp[starter2idx])) {
                        starter1 = false;
                        starter2 = true;
                    }
                    // NOTE: No code needed for the last option (both) since if we selected it,
                    // both are already true...
                }

                // End starter pack selection

                for (int i = 0; i < nPacks; i++) {
                    if (pp[i].charAt(0) == '*') {
                        this.product.add(block.getBooster(pp[i]));
                    }
                    else if ((i == starter1idx && starter1) || (i == starter2idx && starter2)) {
                        this.product.add(new UnOpenedProduct(Singletons.getModel().getTournamentPacks().get(pp[i])));
                    }
                    else {
                        this.product.add(new UnOpenedProduct(Singletons.getModel().getBoosters().get(pp[i])));
                    }
                }
            } else {
                UnOpenedProduct product1;
                if (sets[0].charAt(0) == '*') {
                    product1 = block.getBooster(sets[0]);
                }
                else {
                    product1 = new UnOpenedProduct(Singletons.getModel().getBoosters().get(sets[0]));
                }

                // Choose number of boosters
                final Integer[] integers = new Integer[10];

                for (int i = 0; i < 10; i++) {
                    integers[i] = Integer.valueOf(i + 3);
                }

                Integer nrBoosters = GuiUtils.chooseOne("How many booster packs?", integers);

                for (int i = 0; i < nrBoosters; i++) {
                    this.product.add(product1);
                }
            }

            this.getLandSetCode()[0] = block.getLandSet().getCode();

        } else if (sealedType.equals("Custom")) {
            String[] dList;
            final ArrayList<CustomLimited> customs = new ArrayList<CustomLimited>();

            // get list of custom draft files
            final File dFolder = new File("res/sealed/");
            if (!dFolder.exists()) {
                throw new RuntimeException("GenerateSealed : folder not found -- folder is "
                        + dFolder.getAbsolutePath());
            }

            if (!dFolder.isDirectory()) {
                throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
            }

            dList = dFolder.list();

            for (final String element : dList) {
                if (element.endsWith(".sealed")) {
                    final List<String> dfData = FileUtil.readFile("res/sealed/" + element);
                    final CustomLimited cs = CustomLimited.parse(dfData, Singletons.getModel().getDecks().getCubes());
                    customs.add(cs);
                }
            }

            // present list to user
            if (customs.size() < 1) {
                JOptionPane.showMessageDialog(null, "No custom sealed files found.", "",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                final CustomLimited draft = (CustomLimited) GuiUtils.chooseOne("Choose Custom Sealed Pool",
                        customs);

                final BoosterGenerator bpCustom = new BoosterGenerator(draft.getCardPool());
                final Function<BoosterGenerator, List<CardPrinted>> fnPick = new Function<BoosterGenerator, List<CardPrinted>>() {
                    @Override
                    public List<CardPrinted> apply(final BoosterGenerator pack) {
                        if (draft.getIgnoreRarity()) {
                            if (!draft.getSingleton()) {
                                return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, draft.getNumCards(), 0);
                            } else {
                                return pack.getSingletonBoosterPack(draft.getNumCards());
                            }
                        }
                        return pack.getBoosterPack(draft.getNumbersByRarity(), 0, 0, 0);
                    }
                };

                // Choose number of boosters
                final Integer[] integers = new Integer[10];

                for (int i = 0; i < 10; i++) {
                    integers[i] = Integer.valueOf(i + 3);
                }

                Integer nrBoosters = GuiUtils.chooseOne("How many booster packs?", integers);

                for (int i = 0; i < nrBoosters; i++) {
                    this.product.add(new UnOpenedProduct(fnPick, bpCustom));
                }

                this.getLandSetCode()[0] = draft.getLandSetCode();
            }
        }
    }


    /**
     * <p>
     * getSetCombos.
     * </p>
     * 
     * @return an ArrayList of the set choices.
     */
    private ArrayList<String> getSetCombos(final String[] sets, final int nPacks) {
        ArrayList<String> setCombos = new ArrayList<String>();

        if (nPacks == 4) {
            if (sets.length >= 2) {
                setCombos.add(String.format("%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s", sets[1], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s", sets[1], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s/%s", sets[2], sets[2], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s", sets[2], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 4) {
                setCombos.add(String.format("%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[0]));
            }
        }
        else if (nPacks == 5) {
            if (sets.length >= 2) {
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[1], sets[1], sets[0], sets[0], sets[0]));
            }
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[2], sets[2], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[2], sets[1], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[2], sets[1], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 4) {
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 5) {
                setCombos.add(String.format("%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[0]));
            }
        }
        else if (nPacks == 7 && sets.length >= 7) {
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[5], sets[4], sets[3], sets[2], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[2], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[2], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0]));
        }
        else if (nPacks == 8 && sets.length >= 8) {
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[7], sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[5], sets[4], sets[3], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[5], sets[4], sets[3], sets[2], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[2], sets[2], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0]));

        }
        else if (nPacks == 9 && sets.length >= 9) {
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[8], sets[7], sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[7], sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[6], sets[5], sets[4], sets[3], sets[2], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[3], sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[2], sets[1], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[2], sets[1], sets[1], sets[1], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[2], sets[1], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[2], sets[2], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[1], sets[1], sets[0], sets[0], sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s/%s/%s/%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0], sets[0]));
        }
        else { // Default to 6 packs
            if (sets.length >= 2) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[1], sets[0], sets[0], sets[0]));
            }
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[2], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[1], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 4) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[3], sets[2], sets[1], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 5) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[4], sets[3], sets[2], sets[1], sets[0], sets[0]));
            }
            if (sets.length >= 6) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[5], sets[4], sets[3], sets[2], sets[1], sets[0]));
            }
        }
        return setCombos;
    }
    /**
     * <p>
     * getCardpool.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public ItemPool<CardPrinted> getCardpool() {
        return getCardpool(true);
    }


    /**
     * <p>
     * getCardpool.
     * </p>
     * 
     * @param isHuman
     *      boolean, get pool for human (possible choices)
     * @return a {@link forge.CardList} object.
     */
    public ItemPool<CardPrinted> getCardpool(final boolean isHuman) {

        if (!isHuman) {
            if (!partiality.isEmpty()) {
                partiality.clear();
            }
        }
        final ItemPool<CardPrinted> pool = new ItemPool<CardPrinted>(CardPrinted.class);

        for (int i = 0; i < this.product.size(); i++) {
            pool.addAllFlat(this.product.get(i).open(isHuman, partiality));
        }

        return pool;
    }


    /**
     * Gets the land set code.
     * 
     * @return the landSetCode
     */
    public String[] getLandSetCode() {
        return this.landSetCode;
    }

    /**
     * Sets the land set code.
     * 
     * @param landSetCode0
     *            the landSetCode to set
     */
    public void setLandSetCode(final String[] landSetCode0) {
        this.landSetCode = landSetCode0;
    }

}
