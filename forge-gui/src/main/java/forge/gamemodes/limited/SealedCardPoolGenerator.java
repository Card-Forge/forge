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
package forge.gamemodes.limited;

import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.item.generation.IUnOpenedProduct;
import forge.item.generation.UnOpenedProduct;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.model.UnOpenedMeta;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * <p>
 * SealedDeckFormat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SealedCardPoolGenerator {
    public static final String FILE_EXT = ".sealed";

    private final List<IUnOpenedProduct> product = new ArrayList<>();

    /** The Land set code. */
    private String landSetCode = null;

    public static DeckGroup generateSealedDeck(final boolean addBasicLands) {
        final String prompt = Localizer.getInstance().getMessage("lblChooseSealedDeckFormat");
        final LimitedPoolType poolType = SGuiChoose.oneOrNone(prompt, LimitedPoolType.values());
        if (poolType == null) { return null; }

        SealedCardPoolGenerator sd = new SealedCardPoolGenerator(poolType);
        if (sd.isEmpty()) { return null; }

        final CardPool humanPool = sd.getCardPool(true);
        if (humanPool == null) { return null; }

        // Just assume 7 opponents, allow to play any/all later
        int rounds = 7;

        final String sDeckName = SOptionPane.showInputDialog(
                Localizer.getInstance().getMessage("lblSaveCardPoolAs") + ":",
                Localizer.getInstance().getMessage("lblSaveCardPool"),
                FSkinProp.ICO_QUESTION);

        if (StringUtils.isBlank(sDeckName)) {
            return null;
        }

        final IStorage<DeckGroup> sealedDecks = FModel.getDecks().getSealed();
        if (sealedDecks.contains(sDeckName)) {
            if (!SOptionPane.showConfirmDialog(
                    Localizer.getInstance().getMessage("lblDeckExistsReplaceConfirm", sDeckName),
                    Localizer.getInstance().getMessage("lblSealedDeckGameExists"))) {
                return null;
            }
            sealedDecks.delete(sDeckName);
        }

        final Deck deck = new Deck(sDeckName);
        deck.getOrCreate(DeckSection.Sideboard).addAll(humanPool);

        if (addBasicLands) {
            final int landsCount = 10;
    
            final boolean isZendikarSet = sd.getLandSetCode().equals("ZEN"); // we want to generate one kind of Zendikar lands at a time only
            final boolean zendikarSetMode = MyRandom.getRandom().nextBoolean();
    
            // TODO: Is this still needed? Just use Add Basic Land UI.
            for (final String element : MagicColor.Constant.BASIC_LANDS) {
                int numArt = FModel.getMagicDb().getCommonCards().getArtCount(element, sd.getLandSetCode());
                int minArtIndex = isZendikarSet ? (zendikarSetMode ? 1 : 5) : 1;
                int maxArtIndex = isZendikarSet ? minArtIndex + 3 : numArt;
    
                if (FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_ART_IN_POOLS)) {
                    for (int i = minArtIndex; i <= maxArtIndex; i++) {
                        deck.get(DeckSection.Sideboard).add(element, sd.getLandSetCode(), i, numArt > 1 ? landsCount : 30);
                    }
                }
                else {
                    deck.get(DeckSection.Sideboard).add(element, sd.getLandSetCode(), 30);
                }
            }
        }

        final DeckGroup sealed = new DeckGroup(sDeckName);
        deck.setDirectory(sealedDecks.getName());
        sealed.setHumanDeck(deck);
        for (int i = 0; i < rounds; i++) {
            // Generate other decks for next N opponents
            try {
                final CardPool aiPool = sd.getCardPool(false);
                if (aiPool == null) { return null; }

                sealed.addAiDeck(new SealedDeckBuilder(aiPool.toFlatList()).buildDeck(sd.getLandSetCode()));
            } catch (IllegalStateException e) {
                // If somehow we run out cards to generate pools, stop generating pools
                System.out.println(e.toString());
                rounds = i;
            }
        }

        // Rank the AI decks
        sealed.rankAiDecks(new LimitedDeckEvaluator.LimitedDeckComparer());

        FModel.getDecks().getSealed().add(sealed);
        return sealed;
    }

    /**
     * <p>
     * Constructor for SealedDeck.
     * </p>
     * 
     * @param poolType
     *            a {@link java.lang.String} object.
     */
    private SealedCardPoolGenerator(final LimitedPoolType poolType) {
        switch(poolType) {
            case Full:
                // Choose number of boosters
                if (!chooseNumberOfBoosters(new UnOpenedProduct(SealedProduct.Template.genericDraftBooster))) {
                    return;
                }
                landSetCode = CardEdition.Predicates.getRandomSetWithAllBasicLands(FModel.getMagicDb().getEditions()).getCode();
                break;

            case Prerelease:
                ArrayList<CardEdition> editions = Lists.newArrayList(StaticData.instance().getEditions().getPrereleaseEditions());
                Collections.sort(editions);
                Collections.reverse(editions);

                CardEdition chosenEdition = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseAnEdition"), editions);
                if (chosenEdition == null) {
                    return;
                }

                String bundle = chosenEdition.getPrerelease();

                // Parse prerelease bundle.
                // More recent sets are like 6 boosters of this edition + 1 Promo RareMythic from this edition

                // Expecting to see things like
                // # <Edition> Boosters, # Rarity+

                String[] parts = bundle.split(", ");
                for(String part : parts) {
                    boolean promo = part.endsWith("+");
                    if (promo) {
                        part = part.substring(0, part.length() - 1);
                    }

                    String[] pieces = part.split(" ");
                    int num = Integer.parseInt(pieces[0]);
                    String thing = pieces[pieces.length - 1];

                    // Booster, Rarity, Named, SpecialBooster?

                    if (thing.equalsIgnoreCase("Booster") || thing.equalsIgnoreCase("Boosters")) {
                        // Normal Boosters of this or block editions
                        String code = chosenEdition.getCode();
                        if (pieces.length > 2) {
                            // 2 USG Boosters
                            code = pieces[1];
                        }

                        // Generate draft boosters
                        for(int i = 0; i < num; i++) {
                            this.product.add(new UnOpenedProduct(FModel.getMagicDb().getBoosters().get(code)));
                        }
                    } else {
                        // Rarity
                        List<Pair<String, Integer>> promoSlot = new ArrayList<>();
                        promoSlot.add(Pair.of(pieces[1], num));

                        SealedProduct.Template promoProduct = new SealedProduct.Template("Prerelease Promo", promoSlot);

                        // Create a "booster" with just the promo card. Rarity + Edition into a Template
                        this.product.add(new UnOpenedProduct(promoProduct, FModel.getMagicDb().getCommonCards().getAllCards(chosenEdition)));
                        // TODO This product should be Foiled only. How do I do that?
                    }
                    // TODO Add support for special boosters like GuildPacks
                }

                //chosenEdition but really it should be defined by something in the edition file?
                landSetCode = chosenEdition.getCode();

                break;
            case Block:
            case FantasyBlock:
                List<CardBlock> blocks = new ArrayList<>();
                Iterable<CardBlock> src = poolType == LimitedPoolType.Block ? FModel.getBlocks() : FModel.getFantasyBlocks();
                for (CardBlock b : src) {
                    blocks.add(b);
                }

                final CardBlock block = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseBlock"), blocks);
                if (block == null) { return; }

                final int nPacks = block.getCntBoostersSealed();
                final Stack<String> sets = new Stack<>();

                for (CardEdition edition : block.getSets()) {
                    sets.add(edition.getCode());
                }

                for (String ms : block.getMetaSetNames()) {
                    sets.push(ms);
                }

                if (sets.size() > 1 ) {
                    final List<String> setCombos = getSetCombos(sets, nPacks);
                    if (setCombos == null || setCombos.isEmpty()) {
                        throw new RuntimeException("Unsupported amount of packs (" + nPacks + ") in a Sealed Deck block!");
                    }

                    final String p = setCombos.size() > 1 ? SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChoosePackNumberToPlay"), setCombos) : setCombos.get(0);
                    if (p == null) { return; }

                    for (String pz : TextUtil.split(p, ',')) {
                        String[] pps = TextUtil.splitWithParenthesis(pz.trim(), ' ');
                        String setCode = pps[pps.length - 1];
                        int nBoosters = pps.length > 1 ? Integer.parseInt(pps[0]) : 1;
                        while (nBoosters-- > 0) {
                            this.product.add(block.getBooster(setCode));
                        }
                    }
                }
                else {
                    IUnOpenedProduct prod = block.getBooster(sets.get(0));
                    for (int i = 0; i < nPacks; i++) {
                        this.product.add(prod);
                    }
                }

                landSetCode = block.getLandSet().getCode();
                break;

            case Custom:
                String[] dList;
                final List<CustomLimited> customs = new ArrayList<>();

                // get list of custom draft files
                final File dFolder = new File(ForgeConstants.SEALED_DIR);
                if (!dFolder.exists()) {
                    throw new RuntimeException("GenerateSealed : folder not found -- folder is "
                            + dFolder.getAbsolutePath());
                }

                if (!dFolder.isDirectory()) {
                    throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
                }

                dList = dFolder.list();

                for (final String element : dList) {
                    if (element.endsWith(FILE_EXT)) {
                        final List<String> dfData = FileUtil.readFile(ForgeConstants.SEALED_DIR + element);
                        final CustomLimited cs = CustomLimited.parse(dfData, FModel.getDecks().getCubes());
                        if (cs.getSealedProductTemplate().getNumberOfCardsExpected() > 5) { // Do not allow too small cubes to be played as 'stand-alone'!
                            customs.add(cs);
                        }
                    }
                }

                // present list to user
                if (customs.isEmpty()) {
                    SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblNotFoundCustomSealedFiles"));
                    return;
                }

                final CustomLimited draft = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseCustomSealedPool"), customs);
                if (draft == null) { return; }

                UnOpenedProduct toAdd = new UnOpenedProduct(draft.getSealedProductTemplate(), draft.getCardPool());
                toAdd.setLimitedPool(draft.isSingleton());
                if (!chooseNumberOfBoosters(toAdd)) {
                    return;
                }

                landSetCode = draft.getLandSetCode();
                break;
        }
    }

    private boolean chooseNumberOfBoosters(final IUnOpenedProduct product1) {
        Integer boosterCount = SGuiChoose.getInteger(Localizer.getInstance().getMessage("lblHowManyBoosterPacks"), 3, 12);
        if (boosterCount == null) { return false; }

        for (int i = 0; i < boosterCount; i++) {
            this.product.add(product1);
        }
        return true;
    }

    /**
     * <p>
     * getSetCombos.
     * </p>
     * 
     * @return an ArrayList of the set choices.
     */
    private static List<String> getSetCombos(final List<String> setz, final int nPacks) {
        // TODO These permutations really should be completely generated
        String[] sets = setz.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        List<String> setCombos = new ArrayList<>();

        if (nPacks == 3) {
            if (sets.length >= 2) {
                setCombos.add(TextUtil.concatNoSpace(sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[1], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[1], ", ", sets[1], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace("3 ", sets[1]));
            }
            if (sets.length >= 3) {
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[0], ", ", sets[2], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[2], ", ", sets[2]));
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[1], ", ", sets[0]));
            }
        }
        else if (nPacks == 4) {
            if (sets.length >= 2) {
                setCombos.add(TextUtil.concatNoSpace(sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            }
            if (sets.length >= 3) {
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[2], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            }
            if (sets.length >= 4) {
                setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0]));
            }
        }
        else if (nPacks == 5) {
            if (sets.length == 1 || !sets[0].equals(sets[1]) ) {
                setCombos.add(TextUtil.concatNoSpace("5 ", sets[0]));
            }

            if (sets.length >= 2 && !sets[0].equals(sets[1])) {
                setCombos.add(TextUtil.concatNoSpace("3 ", sets[0], ", 2 ", sets[1]));
                setCombos.add(TextUtil.concatNoSpace("2 ", sets[0], ", 3 ", sets[1]));
            }
            if (sets.length >= 3 && !sets[0].equals(sets[2])) {
                setCombos.add(TextUtil.concatNoSpace("3 ", sets[0], ", 2 ", sets[2]));
                setCombos.add(TextUtil.concatNoSpace("3 ", sets[0], ", ", sets[1], ", ", sets[2]));
                setCombos.add(TextUtil.concatNoSpace("2 ", sets[0], ", 2 ", sets[1], ", ", sets[2]));
            }
            if (sets.length >= 4) {
                if( sets[1].equals(sets[2]) && sets[1].equals(sets[0])) {
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", 4 ", sets[0])); // for guild sealed
                } else {
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", 2 ", sets[0]));
                }
            }
            if (sets.length >= 5) {
                setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0]));
            }
        }
        else if (nPacks == 7 && (sets.length == 4 || sets.length >= 7)) {
            // Sorry. This whole function is awful, it really needs to be rewritten to just generate permutations
            if (sets.length >= 7) {
                setCombos.add(TextUtil.concatNoSpace(sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[2], ", ", sets[2], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[1], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
                setCombos.add(TextUtil.concatNoSpace(sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            } else if (sets.length == 4) {
                if( sets[1].equals(sets[2]) && sets[1].equals(sets[0])) {
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", 6 ", sets[0])); // for origins sealed
                } else {
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", 2 ", sets[2], ", 2 ", sets[1], ", 2 ", sets[0]));
                }
            }
        }
        else if (nPacks == 8 && sets.length >= 8) {
            setCombos.add(TextUtil.concatNoSpace(sets[7], ", ", sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[2], ", ", sets[2], ", ", sets[2], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[1], ", ", sets[1], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
        }
        else if (nPacks == 9 && sets.length >= 9) {
            setCombos.add(TextUtil.concatNoSpace(sets[8], ", ", sets[7], ", ", sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[7], ", ", sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[6], ", ", sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[2], ", ", sets[1], ", ", sets[1], ", ", sets[1], ", ", sets[0], ", ", sets[0], ", ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", 2 ", sets[1], ", 5 ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace("3 ", sets[2], ", 3 ", sets[1], ", 3 ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace("2 ", sets[2], ", 2 ", sets[1], ", 5 ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[2], ", ", sets[1], ", 7 ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace("4 ", sets[2], ", 5 ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace("4 ", sets[1], ", 5 ", sets[0]));
            setCombos.add(TextUtil.concatNoSpace("9 ", sets[0]));
        }
        else { // Default to 6 packs
            if (sets.length == 1 || !sets[0].equals(sets[1]) ) {
                setCombos.add(TextUtil.concatNoSpace("6 ", sets[0]));
            }

            if (sets.length >= 2 && !sets[0].equals(sets[1])) {
                setCombos.add(TextUtil.concatNoSpace("4 ", sets[0], ", 2 ", sets[1]));
                setCombos.add(TextUtil.concatNoSpace("3 ", sets[0], ", 3 ", sets[1]));
                setCombos.add(TextUtil.concatNoSpace("2 ", sets[0], ", 4 ", sets[1]));
            }
            if (sets.length >= 3 && !sets[0].equals(sets[2])) {
                setCombos.add(TextUtil.concatNoSpace("3 ", sets[0], ", 3 ", sets[2]));
                setCombos.add(TextUtil.concatNoSpace("2 ", sets[0], ", 2 ", sets[1], ", 2 ", sets[2]));
            }
            if (sets.length >= 4) {
                if ( sets[1].equals(sets[2]) && sets[1].equals(sets[0])) {
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", 5 ", sets[0])); // for guild sealed
                }
                else {
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", ", sets[1], ", 3 ", sets[0]));
                    setCombos.add(TextUtil.concatNoSpace(sets[3], ", ", sets[2], ", 2 ", sets[1], ", 2 ", sets[0]));
                }
            }
            if (sets.length >= 5) {
                setCombos.add(TextUtil.concatNoSpace(sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", 2 ", sets[0]));
            }
            if (sets.length >= 6) {
                setCombos.add(TextUtil.concatNoSpace( sets[5], ", ", sets[4], ", ", sets[3], ", ", sets[2], ", ", sets[1], ", ", sets[0]));
            }
        }
        return setCombos;
    }

    /**
     * <p>
     * getCardpool.
     * </p>
     * 
     * @param isHuman
     *      boolean, get pool for human (possible choices)
     */
    public CardPool getCardPool(final boolean isHuman) {
        final CardPool pool = new CardPool();

        for (IUnOpenedProduct prod : product) {
            if (prod instanceof UnOpenedMeta) {
                List<PaperCard> cards = ((UnOpenedMeta) prod).open(isHuman, true);
                if (cards == null) {
                    return null; //return null if user canceled
                }
                pool.addAllFlat(cards);
            } else {
                pool.addAllFlat(prod.get());
            }
        }
        return pool;
    }

    /**
     * Gets the land set code.
     * 
     * @return the landSetCode
     */
    public String getLandSetCode() {
        return this.landSetCode;
    }

    public boolean isEmpty() {
        return product.isEmpty();
    }
}
