package forge.screens.deckeditor.controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.DeckRecognizer;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.itemmanager.SItemManagerUtil;
import forge.screens.deckeditor.views.VStatisticsImporter;
import forge.util.ItemPool;
import forge.util.Localizer;

import javax.swing.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CStatisticsImporter {

    private static CStatisticsImporter instance = null;
    private int totalCardsInDecklist = 0;
    private final boolean isCommanderEditor;

    private CStatisticsImporter(boolean isCommanderEditor){
        this.isCommanderEditor = isCommanderEditor;
    }

    public static CStatisticsImporter instance(boolean isCommanderEditor) {
        if (instance == null)
            instance = new CStatisticsImporter(isCommanderEditor);
        return instance;
    }

    public void updateStats(Iterable<DeckRecognizer.Token> cardTokens, boolean includeBannedAndRestricted) {

        List<Map.Entry<PaperCard, Integer>> tokenCards = new ArrayList<>();
        int totalInMain = 0;
        int totalInSide = 0;
        int totalInCommander = 0;
        for (DeckRecognizer.Token token : cardTokens){
            if ((token.getType() == DeckRecognizer.TokenType.LEGAL_CARD) ||
                    (includeBannedAndRestricted && token.getType() == DeckRecognizer.TokenType.LIMITED_CARD)){
                tokenCards.add(new AbstractMap.SimpleEntry<>(token.getCard(), token.getQuantity()));
                if (token.getTokenSection() == DeckSection.Main)
                    totalInMain += token.getQuantity();
                else if (token.getTokenSection() == DeckSection.Sideboard)
                    totalInSide += token.getQuantity();
                else if (token.getTokenSection() == DeckSection.Commander)
                    totalInCommander += token.getQuantity();
            }
        }
        final CardPool deck = new CardPool();
        deck.addAll(tokenCards);

        int total = deck.countAll();
        totalCardsInDecklist = total;
        // Hack-ish: avoid /0 cases, but still populate labels :)
        if (total == 0) { total = 1; }

        VStatisticsImporter vStatsImporter = VStatisticsImporter.instance(this.isCommanderEditor);
        setLabelValue(vStatsImporter.getLblCreature(), deck, CardRulesPredicates.Presets.IS_CREATURE, total);
        setLabelValue(vStatsImporter.getLblLand(), deck, CardRulesPredicates.Presets.IS_LAND, total);
        setLabelValue(vStatsImporter.getLblEnchantment(), deck, CardRulesPredicates.Presets.IS_ENCHANTMENT, total);
        setLabelValue(vStatsImporter.getLblArtifact(), deck, CardRulesPredicates.Presets.IS_ARTIFACT, total);
        setLabelValue(vStatsImporter.getLblInstant(), deck, CardRulesPredicates.Presets.IS_INSTANT, total);
        setLabelValue(vStatsImporter.getLblSorcery(), deck, CardRulesPredicates.Presets.IS_SORCERY, total);
        setLabelValue(vStatsImporter.getLblPlaneswalker(), deck, CardRulesPredicates.Presets.IS_PLANESWALKER, total);

        setLabelValue(vStatsImporter.getLblMulti(), deck, CardRulesPredicates.Presets.IS_MULTICOLOR, total);
        setLabelValue(vStatsImporter.getLblColorless(), deck, CardRulesPredicates.Presets.IS_COLORLESS, total);
        setLabelValue(vStatsImporter.getLblBlack(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLACK), total);
        setLabelValue(vStatsImporter.getLblBlue(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLUE), total);
        setLabelValue(vStatsImporter.getLblGreen(), deck, CardRulesPredicates.isMonoColor(MagicColor.GREEN), total);
        setLabelValue(vStatsImporter.getLblRed(), deck, CardRulesPredicates.isMonoColor(MagicColor.RED), total);
        setLabelValue(vStatsImporter.getLblWhite(), deck, CardRulesPredicates.isMonoColor(MagicColor.WHITE), total);

        setLabelValue(vStatsImporter.getLblCMC0(), deck, SItemManagerUtil.StatTypes.CMC_0.predicate, total);
        setLabelValue(vStatsImporter.getLblCMC1(), deck, SItemManagerUtil.StatTypes.CMC_1.predicate, total);
        setLabelValue(vStatsImporter.getLblCMC2(), deck, SItemManagerUtil.StatTypes.CMC_2.predicate, total);
        setLabelValue(vStatsImporter.getLblCMC3(), deck, SItemManagerUtil.StatTypes.CMC_3.predicate, total);
        setLabelValue(vStatsImporter.getLblCMC4(), deck, SItemManagerUtil.StatTypes.CMC_4.predicate, total);
        setLabelValue(vStatsImporter.getLblCMC5(), deck, SItemManagerUtil.StatTypes.CMC_5.predicate, total);
        setLabelValue(vStatsImporter.getLblCMC6(), deck, SItemManagerUtil.StatTypes.CMC_6.predicate, total);

        vStatsImporter.getLblTotalMain().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalMain").toUpperCase(),
                        totalInMain));
        vStatsImporter.getLblTotalSide().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalSide").toUpperCase(),
                        totalInSide));

        if (this.isCommanderEditor)
            vStatsImporter.getLblTotalCommander().setText(
                    String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalCommander").toUpperCase(),
                            totalInCommander));
        else
            vStatsImporter.getLblTotal().setText(
                    String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalCards").toUpperCase(),
                            deck.countAll()));

    }

    private void setLabelValue(final JLabel label, final ItemPool<PaperCard> deck, final Predicate<CardRules> predicate, final int total) {
        final int tmp = deck.countAll(Predicates.compose(predicate, PaperCard.FN_GET_RULES));
        label.setText(tmp + " (" + calculatePercentage(tmp, total) + "%)");
    }

    public static int calculatePercentage(final int x0, final int y0) {
        return (int) Math.round((double) (x0 * 100) / (double) y0);
    }

    public int getTotalCardsInDecklist(){ return this.totalCardsInDecklist; }
}
