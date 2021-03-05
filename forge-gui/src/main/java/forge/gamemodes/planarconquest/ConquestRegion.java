package forge.gamemodes.planarconquest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.GuiBase;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.deck.generation.DeckGenPool;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.localinstance.assets.ISkinImage;
import forge.model.FModel;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionReader;
import forge.util.collect.FCollectionView;

public class ConquestRegion {
    private final ConquestPlane plane;
    private final String name, artCardName;
    private final ColorSet colorSet;
    private final Predicate<PaperCard> pred;
    private final DeckGenPool cardPool = new DeckGenPool();

    private ISkinImage art;

    private ConquestRegion(ConquestPlane plane0, String name0, String artCardName0, ColorSet colorSet0, Predicate<PaperCard> pred0) {
        plane = plane0;
        name = name0;
        artCardName = artCardName0;
        pred = pred0;
        colorSet = colorSet0;
    }

    public ConquestPlane getPlane() {
        return plane;
    }

    public String getName() {
        return name;
    }

    public void clearArt() {
        art = null;
    }

    public ISkinImage getArt() {
        clearArt(); //force clear this so it will be redrawn since loadingcache invalidates the cache every screen change
        if (art == null) {
            PaperCard pc = cardPool.getCard(artCardName);

            if (pc == null) {
                pc = FModel.getMagicDb().getCommonCards().getUniqueByName(artCardName);
                if (!pc.getName().equals(artCardName) && Card.fromPaperCard(pc, null).hasAlternateState()) {
                    art = GuiBase.getInterface().getCardArt(pc, true);
                } else {
                    art = GuiBase.getInterface().getCardArt(pc);
                }
            } else {
                art = GuiBase.getInterface().getCardArt(pc);
            }
        }

        return art;
    }

    public ColorSet getColorSet() {
        return colorSet;
    }

    public DeckGenPool getCardPool() {
        return cardPool;
    }

    public FCollectionView<PaperCard> getCommanders() {
        FCollection<PaperCard> commanders = new FCollection<>();
        for (PaperCard commander : plane.getCommanders()) {
            if (cardPool.contains(commander)) {
                commanders.add(commander);
            }
        }
        if (commanders.isEmpty()) {
            return plane.getCommanders(); //return all commanders for plane if none found in this region
        }
        return commanders;
    }

    public String toString() {
        return plane.getName().replace("_", " ") + " - " + name;
    }

    public static class Reader extends FCollectionReader<ConquestRegion> {
        private final ConquestPlane plane;

        public Reader(ConquestPlane plane0) {
            super(plane0.getDirectory() + "regions.txt");
            plane = plane0;
        }

        @Override
        protected ConquestRegion read(String line) {
            String name = null;
            String artCardName = null;
            ColorSet colorSet = ColorSet.ALL_COLORS;
            Predicate<PaperCard> pred = Predicates.alwaysTrue();

            String key, value;
            String[] pieces = line.split("\\|");
            for (String piece : pieces) {
                int idx = piece.indexOf(':');
                if (idx != -1) {
                    key = piece.substring(0, idx).trim().toLowerCase();
                    value = piece.substring(idx + 1).trim();
                }
                else {
                    alertInvalidLine(line, "Invalid region definition.");
                    key = piece.trim().toLowerCase();
                    value = "";
                }
                switch(key) {
                case "name":
                    name = value;
                    break;
                case "art":
                    artCardName = value;
                    break;
                case "colors":
                    colorSet = ColorSet.fromNames(value.toCharArray());
                    pred = Predicates.compose(CardRulesPredicates.hasColorIdentity(colorSet.getColor()), PaperCard.FN_GET_RULES);
                    break;
                case "sets":
                    final String[] sets = value.split(",");
                    for (int i = 0; i < sets.length; i++) {
                        sets[i] = sets[i].trim();
                    }
                    pred = new Predicate<PaperCard>() {
                        @Override
                        public boolean apply(PaperCard pc) {
                            for (String set : sets) {
                                if (pc.getEdition().equals(set)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    };
                    break;
                default:
                    alertInvalidLine(line, "Invalid region definition.");
                    break;
                }
            }
            return new ConquestRegion(plane, name, artCardName, colorSet, pred);
        }
    }

    static void addCard(PaperCard pc, Iterable<ConquestRegion> regions) {
        boolean foundRegion = false;
        for (ConquestRegion region : regions) {
            if (region.pred.apply(pc)) {
                region.cardPool.add(pc);
                foundRegion = true;
            }
        }

        if (foundRegion) { return; }

        //if card doesn't match any region's predicate, make card available to all regions
        for (ConquestRegion region : regions) {
            region.cardPool.add(pc);
        }
    }

    public static Set<ConquestRegion> getAllRegionsOfCard(PaperCard card) {
        Set<ConquestRegion> regions = new HashSet<>();
        for (ConquestPlane plane : FModel.getPlanes()) {
            if (plane.getCardPool().contains(card)) {
                for (ConquestRegion region : plane.getRegions()) {
                    if (region.getCardPool().contains(card)) {
                        regions.add(region);
                    }
                }
            }
        }
        return regions;
    }

    public static List<ConquestRegion> getAllRegions() {
        List<ConquestRegion> regions = new ArrayList<>();
        for (ConquestPlane plane : FModel.getPlanes()) {
            for (ConquestRegion region : plane.getRegions()) {
                regions.add(region);
            }
        }
        return regions;
    }
}