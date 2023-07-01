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

    private int totalCardsInDecklist = 0;
    private final boolean isCommanderEditor;
    private final VStatisticsImporter view;

    public CStatisticsImporter(VStatisticsImporter vStatisticsImporter){
        this.view = vStatisticsImporter;
        this.isCommanderEditor = this.view.isViewForCommanderEditor();
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

        setLabelValue(this.view.getLblCreature(), deck, CardRulesPredicates.Presets.IS_CREATURE, total);
        setLabelValue(this.view.getLblLand(), deck, CardRulesPredicates.Presets.IS_LAND, total);
        setLabelValue(this.view.getLblEnchantment(), deck, CardRulesPredicates.Presets.IS_ENCHANTMENT, total);
        setLabelValue(this.view.getLblArtifact(), deck, CardRulesPredicates.Presets.IS_ARTIFACT, total);
        setLabelValue(this.view.getLblInstant(), deck, CardRulesPredicates.Presets.IS_INSTANT, total);
        setLabelValue(this.view.getLblSorcery(), deck, CardRulesPredicates.Presets.IS_SORCERY, total);
        setLabelValue(this.view.getLblPlaneswalker(), deck, CardRulesPredicates.Presets.IS_PLANESWALKER, total);

        setLabelValue(this.view.getLblMulti(), deck, CardRulesPredicates.Presets.IS_MULTICOLOR, total);
        setLabelValue(this.view.getLblColorless(), deck, CardRulesPredicates.Presets.IS_COLORLESS, total);
        setLabelValue(this.view.getLblBlack(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLACK), total);
        setLabelValue(this.view.getLblBlue(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLUE), total);
        setLabelValue(this.view.getLblGreen(), deck, CardRulesPredicates.isMonoColor(MagicColor.GREEN), total);
        setLabelValue(this.view.getLblRed(), deck, CardRulesPredicates.isMonoColor(MagicColor.RED), total);
        setLabelValue(this.view.getLblWhite(), deck, CardRulesPredicates.isMonoColor(MagicColor.WHITE), total);

        setLabelValue(this.view.getLblCMC0(), deck, SItemManagerUtil.StatTypes.CMC_0.predicate, total);
        setLabelValue(this.view.getLblCMC1(), deck, SItemManagerUtil.StatTypes.CMC_1.predicate, total);
        setLabelValue(this.view.getLblCMC2(), deck, SItemManagerUtil.StatTypes.CMC_2.predicate, total);
        setLabelValue(this.view.getLblCMC3(), deck, SItemManagerUtil.StatTypes.CMC_3.predicate, total);
        setLabelValue(this.view.getLblCMC4(), deck, SItemManagerUtil.StatTypes.CMC_4.predicate, total);
        setLabelValue(this.view.getLblCMC5(), deck, SItemManagerUtil.StatTypes.CMC_5.predicate, total);
        setLabelValue(this.view.getLblCMC6(), deck, SItemManagerUtil.StatTypes.CMC_6.predicate, total);

        this.view.getLblTotalMain().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalMain").toUpperCase(),
                        totalInMain));
        this.view.getLblTotalSide().setText(
                String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalSide").toUpperCase(),
                        totalInSide));

        if (this.isCommanderEditor)
            this.view.getLblTotalCommander().setText(
                    String.format("%s: %d", Localizer.getInstance().getMessage("lblTotalCommander").toUpperCase(),
                            totalInCommander));
        else
            this.view.getLblTotal().setText(
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
