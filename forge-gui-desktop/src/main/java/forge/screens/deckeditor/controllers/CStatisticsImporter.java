package forge.screens.deckeditor.controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.itemmanager.SItemManagerUtil;
import forge.screens.deckeditor.views.VStatisticsImporter;
import forge.util.ItemPool;

import javax.swing.*;
import java.util.Map;

public class CStatisticsImporter {

    private static CStatisticsImporter instance = null;

    private CStatisticsImporter(){}

    public static CStatisticsImporter instance() {
        if (instance == null)
            instance = new CStatisticsImporter();
        return instance;
    }

    public void updateStats(Iterable<Map.Entry<PaperCard, Integer>> tokenCards) {
        final CardPool deck = new CardPool();
        deck.addAll(tokenCards);

        int total = deck.countAll();
        final int[] shardCount = calculateShards(deck);

        // Hack-ish: avoid /0 cases, but still populate labels :)
        if (total == 0) { total = 1; }

        setLabelValue(VStatisticsImporter.instance().getLblCreature(), deck, CardRulesPredicates.Presets.IS_CREATURE, total);
        setLabelValue(VStatisticsImporter.instance().getLblLand(), deck, CardRulesPredicates.Presets.IS_LAND, total);
        setLabelValue(VStatisticsImporter.instance().getLblEnchantment(), deck, CardRulesPredicates.Presets.IS_ENCHANTMENT, total);
        setLabelValue(VStatisticsImporter.instance().getLblArtifact(), deck, CardRulesPredicates.Presets.IS_ARTIFACT, total);
        setLabelValue(VStatisticsImporter.instance().getLblInstant(), deck, CardRulesPredicates.Presets.IS_INSTANT, total);
        setLabelValue(VStatisticsImporter.instance().getLblSorcery(), deck, CardRulesPredicates.Presets.IS_SORCERY, total);
        setLabelValue(VStatisticsImporter.instance().getLblPlaneswalker(), deck, CardRulesPredicates.Presets.IS_PLANESWALKER, total);

        setLabelValue(VStatisticsImporter.instance().getLblMulti(), deck, CardRulesPredicates.Presets.IS_MULTICOLOR, total);
        setLabelValue(VStatisticsImporter.instance().getLblColorless(), deck, CardRulesPredicates.Presets.IS_COLORLESS, total);
        setLabelValue(VStatisticsImporter.instance().getLblBlack(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLACK), total);
        setLabelValue(VStatisticsImporter.instance().getLblBlue(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLUE), total);
        setLabelValue(VStatisticsImporter.instance().getLblGreen(), deck, CardRulesPredicates.isMonoColor(MagicColor.GREEN), total);
        setLabelValue(VStatisticsImporter.instance().getLblRed(), deck, CardRulesPredicates.isMonoColor(MagicColor.RED), total);
        setLabelValue(VStatisticsImporter.instance().getLblWhite(), deck, CardRulesPredicates.isMonoColor(MagicColor.WHITE), total);

        setLabelValue(VStatisticsImporter.instance().getLblCMC0(), deck, SItemManagerUtil.StatTypes.CMC_0.predicate, total);
        setLabelValue(VStatisticsImporter.instance().getLblCMC1(), deck, SItemManagerUtil.StatTypes.CMC_1.predicate, total);
        setLabelValue(VStatisticsImporter.instance().getLblCMC2(), deck, SItemManagerUtil.StatTypes.CMC_2.predicate, total);
        setLabelValue(VStatisticsImporter.instance().getLblCMC3(), deck, SItemManagerUtil.StatTypes.CMC_3.predicate, total);
        setLabelValue(VStatisticsImporter.instance().getLblCMC4(), deck, SItemManagerUtil.StatTypes.CMC_4.predicate, total);
        setLabelValue(VStatisticsImporter.instance().getLblCMC5(), deck, SItemManagerUtil.StatTypes.CMC_5.predicate, total);
        setLabelValue(VStatisticsImporter.instance().getLblCMC6(), deck, SItemManagerUtil.StatTypes.CMC_6.predicate, total);

        int totShards = calculateTotalShards(shardCount);
        setLabelValue(VStatisticsImporter.instance().getLblWhiteShard(), "Shards:", shardCount[0], totShards);
        setLabelValue(VStatisticsImporter.instance().getLblBlueShard(), "Shards:", shardCount[1], totShards);
        setLabelValue(VStatisticsImporter.instance().getLblBlackShard(), "Shards:", shardCount[2], totShards);
        setLabelValue(VStatisticsImporter.instance().getLblRedShard(), "Shards:", shardCount[3], totShards);
        setLabelValue(VStatisticsImporter.instance().getLblGreenShard(), "Shards:", shardCount[4], totShards);
        setLabelValue(VStatisticsImporter.instance().getLblColorlessShard(), "Shards:", shardCount[5], totShards);
    }

    private void setLabelValue(final JLabel label, final ItemPool<PaperCard> deck, final Predicate<CardRules> predicate, final int total) {
        final int tmp = deck.countAll(Predicates.compose(predicate, PaperCard.FN_GET_RULES));
        label.setText(tmp + " (" + calculatePercentage(tmp, total) + "%)");
    }

    private void setLabelValue(final JLabel label, final String str, final int value, final int total) {
        String labelText = String.format("%s%d (%d%%)", str, value, calculatePercentage(value, total));
        label.setText(labelText);
    }

    public static int calculatePercentage(final int x0, final int y0) {
        return (int) Math.round((double) (x0 * 100) / (double) y0);
    }

    public static int[] calculateShards(final ItemPool<PaperCard> deck) {
        final int[] counts = new int[6]; // in WUBRGC order
        for (final PaperCard c : deck.toFlatList()) {
            final int[] cShards = c.getRules().getManaCost().getColorShardCounts();
            for (int i = 0; i < 6; i++) {
                counts[i] += cShards[i];
            }
        }
        return counts;
    }

    public static int calculateTotalShards(int[] counts) {
        int total = 0;
        for (int count : counts) {
            total += count;
        }
        return total;
    }
}
