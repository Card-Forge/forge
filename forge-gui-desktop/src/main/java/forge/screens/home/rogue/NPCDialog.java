package forge.screens.home.rogue;

import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCContext.NPCChoice;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog for NPC encounters. Shows avatar, flavor text, and npcEffect choice buttons.
 */
public class NPCDialog {

    private static final int DIALOG_WIDTH = 600;
    private static final int MIN_DIALOG_HEIGHT = 480;
    private static final int PANEL_INSETS = 20;
    private static final int FULL_WIDTH = DIALOG_WIDTH - 2 * PANEL_INSETS;
    private static final int REROLL_OPTION = 0;
    private static final int CHOICE_RESULT = 1;
    private static final float FLAVOR_FONT_SIZE = 16f;

    private final MainPanel panel;
    private final ChoiceRerollContext rerollCtx;
    private final List<String> flavorTextChunks;
    private final List<FButton> choiceButtons = new ArrayList<>();
    private final List<PreviewTarget> previewTargets = new ArrayList<>();
    private final RogueUIHelper.TypewriterText typewriterText;
    private FButton btnContinue;
    private FOptionPane optionPane;
    private RoguePreviewPopup previewPopup;
    private NPCEffect selectedBoon;
    private int currentChunkIndex;

    public NPCDialog(NPCContext ctx, ChoiceRerollContext rerollCtx) {
        this.rerollCtx = rerollCtx;
        panel = new MainPanel();
        flavorTextChunks = ctx.flavorTextChunks();
        String firstFlavorText = flavorTextChunks.get(0);

        FLabel lblTitle = new FLabel.Builder()
                .text(ctx.displayName())
                .fontSize(20).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build();

        FLabel lblAvatar = new FLabel.Builder().build();
        lblAvatar.setIcon(FSkin.getAvatars().get(RogueUIHelper.getAvailableAvatarIndex(ctx.avatarIndex())));

        FTextArea txtFlavor = new FTextArea(firstFlavorText);
        txtFlavor.setFont(txtFlavor.getFont().deriveFont(FLAVOR_FONT_SIZE));
        typewriterText = RogueUIHelper.prepareTypewriterText(txtFlavor, panel, flavorTextChunks, FULL_WIDTH);
        int choiceButtonWidth = FULL_WIDTH * 4 / 5;

        int desiredHeight = PANEL_INSETS;

        panel.add(lblTitle, "w 100%!, h 40px!, ax center, gap 0 0 20px 10px, wrap");
        desiredHeight += 40 + 20 + 10;

        panel.add(lblAvatar, "w 100px!, h 100px!, ax center, gap 0 0 10px 10px, wrap");
        desiredHeight += 100 + 10 + 10;

        panel.add(txtFlavor, "w 100%!, ax center, gap 0 0 10px 20px, wrap");
        desiredHeight += txtFlavor.getPreferredSize().height + 10 + 20;

        if (flavorTextChunks.size() > 1 || ctx.choices().isEmpty()) {
            btnContinue = RogueButtonHelper.createNavigationButton(Localizer.getInstance().getMessage("lblContinue"));
            RogueButtonHelper.setChoiceButtonSizeHint(btnContinue, choiceButtonWidth);
            btnContinue.addActionListener(e -> advanceChunkOrClose());
            panel.add(btnContinue, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
            desiredHeight += btnContinue.getPreferredSize().height + 10 + 10;
        }

        for (NPCChoice choice : ctx.choices()) {
            String desc = choice.npcEffect() != null ? choice.npcEffect().getDescription() : "";
            FButton btn = RogueButtonHelper.createChoiceButton(choice.label(), desc,
                    choice.npcEffect() == null ? List.of() : choice.npcEffect().getPreviewReferences());
            RogueButtonHelper.setChoiceButtonSizeHint(btn, choiceButtonWidth);
            btn.addActionListener(e -> {
                hidePreview();
                selectedBoon = choice.npcEffect();
                optionPane.setResult(CHOICE_RESULT);
                optionPane.setVisible(false);
            });
            choiceButtons.add(btn);
            previewTargets.add(new PreviewTarget(btn,
                    choice.npcEffect() == null ? List.of() : choice.npcEffect().getPreviewReferences()));
            panel.add(btn, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
            desiredHeight += btn.getPreferredSize().height + 10 + 10;
        }
        updateButtonVisibility();

        int dialogHeight = Math.min(Math.max(desiredHeight + PANEL_INSETS, MIN_DIALOG_HEIGHT), getMaxDialogHeight());
        Dimension dialogSize = new Dimension(DIALOG_WIDTH, dialogHeight);
        panel.setPreferredSize(dialogSize);
        panel.setMinimumSize(dialogSize);
    }

    /** Show dialog and return the selected action. */
    public DialogResult show() {
        selectedBoon = null;
        currentChunkIndex = 0;
        typewriterText.setFullText(flavorTextChunks.get(currentChunkIndex));
        updateButtonVisibility();
        boolean hasRerolls = rerollCtx.remainingRerolls > 0;
        optionPane = new FOptionPane(null, Localizer.getInstance().getMessage("lblRogueNpcEncounter"), null, panel,
                hasRerolls ? List.of(Localizer.getInstance().getMessage("lblRogueRerollsRemaining", rerollCtx.remainingRerolls)) : List.of(), -1);
        optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        RogueUIHelper.disableSpacebarButtonActivation(optionPane.getContentPane());
        optionPane.getTitleBar().setVisible(false);
        previewPopup = new RoguePreviewPopup();
        previewTargets.forEach(target -> previewPopup.attachTo(target.component(), target.references()));
        panel.revalidate();
        panel.repaint();
        typewriterText.start();
        optionPane.setVisible(true);
        int result = optionPane.getResult();
        typewriterText.stop();
        hidePreview();
        optionPane.dispose();
        return new DialogResult(result == REROLL_OPTION && hasRerolls, selectedBoon);
    }

    private void advanceChunkOrClose() {
        hidePreview();
        if (currentChunkIndex < flavorTextChunks.size() - 1) {
            currentChunkIndex++;
            typewriterText.setFullText(flavorTextChunks.get(currentChunkIndex));
            updateButtonVisibility();
            panel.revalidate();
            panel.repaint();
            typewriterText.start();
            return;
        }

        optionPane.setResult(CHOICE_RESULT);
        optionPane.setVisible(false);
    }

    private void updateButtonVisibility() {
        boolean isFinalChunk = currentChunkIndex >= flavorTextChunks.size() - 1;
        if (btnContinue != null) {
            btnContinue.setVisible(!isFinalChunk || choiceButtons.isEmpty());
        }
        for (FButton choiceButton : choiceButtons) {
            choiceButton.setVisible(isFinalChunk);
        }
    }

    private void hidePreview() {
        if (previewPopup != null) {
            previewPopup.hide();
        }
    }

    private static int getMaxDialogHeight() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;
        return (int) (usableHeight * 0.9) - 80;
    }

    private record PreviewTarget(JComponent component, List<PreviewReference> references) {}

    public record DialogResult(boolean rerollRequested, NPCEffect choice) {}

    private static class MainPanel extends SkinnedPanel {
        private MainPanel() {
            super(new MigLayout("insets 20, gap 0, wrap, align center", "[grow, center]", ""));
            setOpaque(false);
        }
    }
}
