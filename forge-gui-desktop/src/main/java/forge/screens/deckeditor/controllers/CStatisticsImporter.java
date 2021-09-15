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

    private CStatisticsImporter(){}

    public static CStatisticsImporter instance() {
        if (instance == null)
            instance = new CStatisticsImporter();
        return instance;
    }

    public void updateStats(Iterable<DeckRecognizer.Token> cardTokens) {

        List<Map.Entry<PaperCard, Integer>> tokenCards = new ArrayList();
        int totalInMain = 0;
        int totalInSide = 0;
        for (DeckRecognizer.Token token : cardTokens){
            if (token.getType() == DeckRecognizer.TokenType.LEGAL_CARD_REQUEST) {
                tokenCards.add(new AbstractMap.SimpleEntry<>(token.getCard(), token.getNumber()));
                if (token.getTokenSection().equals(DeckSection.Main))
                    totalInMain += token.getNumber();
                else if (token.getTokenSection().equals(DeckSection.Sideboard))
                    totalInSide += token.getNumber();
            }
        }
        final CardPool deck = new CardPool();
        deck.addAll(tokenCards);

        int total = deck.countAll();
        totalCardsInDecklist = total;
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

        VStatisticsImporter.instance().getLblTotal().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalCards").toUpperCase(),
                        deck.countAll()));
        VStatisticsImporter.instance().getLblTotalMain().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalMain").toUpperCase(),
                        totalInMain));
        VStatisticsImporter.instance().getLblTotalSide().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalSide").toUpperCase(),
                        totalInSide));
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
