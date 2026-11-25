package forge.screens.home.rogue;

import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.CursedEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.WrathfulEffect;
import forge.gamemodes.rogue.flow.RogueDecisions;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.screens.home.CHomeUI;
import forge.screens.home.sanctioned.AvatarSelector;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

/** Asks and announces with Swing dialogs. */
class DesktopRogueDecisions implements RogueDecisions {

    @Override
    public List<PaperCard> chooseCommanders() {
        return new CommanderSelectionDialog().show();
    }

    @Override
    public int choosePortrait(PaperCard commander, int current) {
        int[] chosen = {current};
        AvatarSelector avatarSelector = new AvatarSelector(commander.getName(),
            RogueUIHelper.getAvailableAvatarIndex(current), List.of());
        for (FLabel avatar : avatarSelector.getSelectables()) {
            int avatarIndex = Integer.parseInt(avatar.getName().substring(11));
            if (RogueUIHelper.getAvailableAvatarIndex(avatarIndex) != avatarIndex) {
                avatar.setVisible(false);
                continue;
            }
            avatar.setCommand((UiCommand) () -> {
                chosen[0] = avatarIndex;
                avatarSelector.setVisible(false);
            });
        }
        avatarSelector.setVisible(true);
        avatarSelector.dispose();
        return chosen[0];
    }

    @Override
    public List<PaperCard> chooseCards(CardChoice choice) {
        return new CardSelectionDialog(choice.title(), choice.subtitle(), choice.candidates(), choice.run(),
            choice.min(), choice.max(), choice.preview()).show();
    }

    @Override
    public boolean confirmReplaceRun() {
        Localizer localizer = Localizer.getInstance();
        return FOptionPane.showConfirmDialog(
            localizer.getMessage("lblRogueAbandonForNewRun"),
            localizer.getMessage("lblRogueAbandonCurrentRun"),
            localizer.getMessage("lblRogueStartNewRun"),
            localizer.getMessage("lblCancel"),
            false
        );
    }

    @Override
    public Pick<NPCEffect> chooseBoon(NPCContext context, ChoiceRerollContext rerolls) {
        return new NPCDialog(context, rerolls).show();
    }

    @Override
    public Pick<ChestEffect> chooseChest(List<ChestEffect> loot, RogueRun run, ChoiceRerollContext rerolls) {
        return new ChestDialog(loot, run, rerolls).show();
    }

    @Override
    public Pick<RogueEvent.EventChoice> chooseEvent(RogueEvent event, RogueRun run, ChoiceRerollContext rerolls) {
        return new EventDialog(event, run, rerolls).show();
    }

    @Override
    public SanctumAnswer chooseSanctum(SanctumOffer offer) {
        return new SanctumDialog(offer.options()).show();
    }

    @Override
    public ShopAnswer shop(ShopOffer offer) {
        BazaarDialog dialog = new BazaarDialog(
            offer.items(),
            offer.run(),
            offer.gold(),
            offer.title(),
            offer.rerollLabel(),
            offer.tab());
        dialog.setRerollEnabled(offer.rerollEnabled());
        return dialog.show();
    }

    @Override
    public List<PaperCard> show(String title, List<PaperCard> cards, int maxSelections, String rerollLabel,
                                int rerollCost, boolean rerollEnabled, int gold) {
        return new CardRewardDialog(title, cards, maxSelections, rerollLabel, rerollEnabled, gold).show();
    }

    @Override
    public <T> List<T> devPick(String title, List<T> all, List<T> current, int count) {
        if (count == 1) {
            return Collections.singletonList(RogueUIHelper.chooseDevOverride(title, all, current.get(0)));
        }
        boolean chest = all.get(0) instanceof ChestEffect;
        List<T> selected = RogueUIHelper.chooseDevEffects(title,
            Localizer.getInstance().getMessage(chest ? "lblRogueDevOverrideChestLoot" : "lblRogueDevOverrideNpcChoices"),
            chest ? "lblRogueChestOverrideSelection" : "lblRogueNpcOverrideSelection",
            all, current, count);
        return selected.isEmpty() ? current : selected;
    }

    @Override
    public void showPlaneboundEffects(NodePlanebound node, List<WrathfulEffect> wrathful, List<CursedEffect> cursed) {
        ImageIcon flameIcon = NodePlaneboundPanel.createFlameIcon(14, 18);
        ImageIcon pentagramIcon = NodePlaneboundPanel.createPentagramIcon(14, 18);
        FSkin.SkinnedPanel effectsPanel = new FSkin.SkinnedPanel(
            new MigLayout("insets 5, gap 0, wrap", "[grow]"));
        effectsPanel.setOpaque(false);

        for (WrathfulEffect w : wrathful) {
            createEffectLabel(flameIcon, effectsPanel, w.getDisplayName(), w.getDescription());
        }
        for (CursedEffect c : cursed) {
            createEffectLabel(pentagramIcon, effectsPanel, c.getDisplayName(), c.getDescription());
        }

        Heading heading = RogueDecisions.planeboundEffects(wrathful, cursed);
        FOptionPane.showOptionDialog(heading.text(), heading.title(),
            null, effectsPanel, List.of(Localizer.getInstance().getMessage("lblOK")), 0);
    }

    private void createEffectLabel(ImageIcon pentagramIcon, SkinnedPanel effectsPanel,
        String displayName, String description) {
        JLabel lbl = new JLabel(displayName + " - " + description, pentagramIcon, SwingConstants.LEFT);
        lbl.setFont(FSkin.getRelativeFont(12).getBaseFont());
        lbl.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        lbl.setIconTextGap(5);
        lbl.setOpaque(false);
        effectsPanel.add(lbl, "growx, h 24px!, wrap");
    }

    @Override
    public void showResult(ResultKind kind, String title, String text, List<Section> sections) {
        switch (kind) {
            case EVENT -> showDefaultNodeResultDialog(title, text, sections, true);
            case BOON -> showDefaultNodeResultDialog(title, text, sections, false);
            case SANCTUM -> showResultDialog(title, text, sections, 900, 700,
                NodeResultPanel.MessageAlignment.CENTER, false);
            case REVIVE, SPARK -> showMessage(title, text);
            case DEFEAT -> showSideNodeDefeatDialog(title, text);
        }
    }

    private void showSideNodeDefeatDialog(String title, String message) {
        NodeResultPanel resultPanel = new NodeResultPanel(message, List.of());
        FOptionPane optionPane = new FOptionPane(null, title, null, resultPanel, List.of(Localizer.getInstance().getMessage("lblOK")), 0);
        resultPanel.initZoom(optionPane);
        optionPane.setVisible(true);
        optionPane.dispose();
    }

    private void showDefaultNodeResultDialog(String title, String message,
                                             List<Section> sections,
                                             boolean typewriterMessage) {
        boolean hasCardSections = sections.stream().anyMatch(
            section -> section.cards() != null && !section.cards().isEmpty());
        int minHeight = hasCardSections ? 700 : 0;
        showResultDialog(title, message, sections, 650, minHeight,
            NodeResultPanel.MessageAlignment.LEFT, typewriterMessage);
    }

    private void showResultDialog(String title, String message,
                                  List<Section> sections,
                                  int minWidth, int minHeight,
                                  NodeResultPanel.MessageAlignment messageAlignment,
                                  boolean typewriterMessage) {
        NodeResultPanel resultPanel = new NodeResultPanel(
            message, sections, minWidth, minHeight, messageAlignment, typewriterMessage);
        FScrollPane scrollPane = new FScrollPane(resultPanel, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Dimension resultSize = resultPanel.getPreferredSize();
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int maxDialogWidth = (int) ((screenBounds.width - screenInsets.left - screenInsets.right) * 0.9) - 50;
        int maxDialogHeight = (int) ((screenBounds.height - screenInsets.top - screenInsets.bottom) * 0.9) - 80;
        Dimension dialogSize = new Dimension(
            Math.min(resultSize.width + 30, maxDialogWidth),
            Math.min(resultSize.height, maxDialogHeight));

        FSkin.SkinnedPanel wrapper = new FSkin.SkinnedPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        wrapper.setPreferredSize(dialogSize);
        wrapper.setMinimumSize(dialogSize);

        FOptionPane optionPane = new FOptionPane(null, title, null, wrapper, List.of(Localizer.getInstance().getMessage("lblOK")), 0);
        resultPanel.initZoom(optionPane);
        resultPanel.startTypewriter();
        optionPane.setVisible(true);
        resultPanel.stopTypewriter();
        optionPane.dispose();
    }

    @Override
    public void showMessage(String title, String text) {
        FOptionPane.showMessageDialog(text, title);
    }

    @Override
    public void showTutorial(RogueTutorial tutorial) {
        RogueTutorialHelper.showIfNotSeen(tutorial);
    }

    @Override
    public void startMatch(RogueMatch match) throws Exception {
        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        try {
            RogueRun run = match.run();
            if (!run.getCarryCards().isEmpty()) {
                showTutorial(RogueTutorial.CARRY_CARDS);
            }
            HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
            if (!RogueTutorialHelper.hasSeenTutorial(RogueTutorial.MATCH_UI)
                    || !RogueTutorialHelper.hasSeenTutorial(RogueTutorial.MATCH_CARD_HIGHLIGHTING)
                    || !RogueTutorialHelper.hasSeenTutorial(RogueTutorial.MATCH_PHASES_AND_YIELDS)) {
                hostedMatch.setStartGameHook(() -> {
                    RogueTutorialHelper.registerMatchTutorials(hostedMatch.getGame());
                    FThreads.invokeInEdtAndWait(() -> RogueTutorialHelper.showIfNotSeen(RogueTutorial.MATCH_UI));
                    hostedMatch.setStartGameHook(null);
                });
            }
            hostedMatch.setEndGameHook(() -> match.recordPublicCards().accept(hostedMatch));
            run.setHostedMatch(hostedMatch);

            hostedMatch.startMatch(
                match.rules(),
                match.variants(),
                match.players(),
                match.human(),
                GuiBase.getInterface().getNewGuiGame()
            );
        } finally {
            SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
        }
    }

    @Override
    public void mapChanged(RogueRun run) {
        CSubmenuRogueMap.SINGLETON_INSTANCE.update();
    }

    @Override
    public void runStarted(RogueRun run) {
        CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEMAP);
    }

    @Override
    public void runEnded() {
        CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
    }
}
