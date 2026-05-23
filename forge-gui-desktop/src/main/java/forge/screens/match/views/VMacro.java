package forge.screens.match.views;

import java.awt.Font;
import java.awt.Container;
import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.interfaces.IMacroSystem;
import forge.screens.match.controllers.CMacro;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public class VMacro implements IVDoc<CMacro> {
    private final CMacro controller;
    private final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblMacroWindow"));
    private final JPanel contentPanel = new JPanel();
    private final JLabel statusLabel;
    private final JLabel detailLabel;
    private final JLabel logLabel;
    private final JPanel emptyLogPanel = new JPanel();
    private final JPanel logPanel = new JPanel();
    private final FTextArea actionText = new FTextArea();
    private final FTextArea logText = new FTextArea();
    private final FScrollPane actionScroller = new FScrollPane(actionText, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final FScrollPane logScroller = new FScrollPane(logText, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JSplitPane actionLogSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, actionScroller, logPanel);
    private final Highlighter.HighlightPainter activeStepPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 225, 120, 120));
    private List<String> displayedActions;
    private List<String> displayedMessages;
    private final List<Integer> actionStartOffsets = new ArrayList<>();
    private final List<Integer> actionEndOffsets = new ArrayList<>();
    private int displayedActiveActionIndex = -2;
    private DragCell parentCell;

    public VMacro(final CMacro controller) {
        this.controller = controller;
        statusLabel = new FLabel.Builder()
                .fontSize(13)
                .fontStyle(Font.BOLD)
                .fontAlign(SwingConstants.LEFT)
                .opaque()
                .build();
        detailLabel = new FLabel.Builder()
                .fontSize(12)
                .fontStyle(Font.PLAIN)
                .fontAlign(SwingConstants.LEFT)
                .opaque()
                .build();
        logLabel = new FLabel.Builder()
                .fontSize(12)
                .fontStyle(Font.BOLD)
                .fontAlign(SwingConstants.LEFT)
                .opaque()
                .build();
        statusLabel.setOpaque(false);
        detailLabel.setOpaque(false);
        logLabel.setOpaque(false);
        logLabel.setText(localizer.getMessage("lblMacroLog"));
        contentPanel.setOpaque(false);
        emptyLogPanel.setOpaque(false);
        logPanel.setOpaque(false);
        logPanel.setLayout(new MigLayout("wrap 1, gap 2px!, insets 0"));
        logPanel.add(logLabel, "w 100%, h 20px!");
        logPanel.add(logScroller, "w 100%, h 0:100%");
        configureCopyTextArea(actionText);
        configureCopyTextArea(logText);
        actionLogSplit.setOpaque(false);
        actionLogSplit.setBorder(null);
        actionLogSplit.setResizeWeight(0.75);
        actionLogSplit.setBottomComponent(emptyLogPanel);
        actionLogSplit.setDividerSize(0);
        actionLogSplit.setOneTouchExpandable(false);
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final JPopupMenu menu = new JPopupMenu();
                    final JMenuItem undock = new JMenuItem(localizer.getMessage("lblUndock"));
                    undock.addActionListener(ev -> controller.showFloatingWindow());
                    menu.add(undock);
                    menu.show(tab, e.getX(), e.getY());
                }
            }
        });
    }

    private void configureCopyTextArea(final FTextArea textArea) {
        textArea.setLineWrap(true);
        textArea.setFocusable(false);
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                textArea.setFocusable(true);
                textArea.requestFocusInWindow();
            }
        });
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                textArea.setFocusable(false);
            }
        });
    }

    @Override
    public void populate() {
        populateContainer(parentCell.getBody());
    }

    public void populateContainer(final Container container) {
        contentPanel.removeAll();
        contentPanel.setLayout(new MigLayout("wrap 1, gap 4px!, insets 4px"));
        contentPanel.add(statusLabel, "w 100%, h 24px!");
        contentPanel.add(detailLabel, "w 100%, h 22px!");
        contentPanel.add(actionLogSplit, "w 100%, h 0:100%");
        if (container instanceof JPanel panel) {
            panel.removeAll();
            panel.setLayout(new MigLayout("insets 0, gap 0"));
            panel.add(contentPanel, "w 100%, h 100%");
        } else {
            container.remove(contentPanel);
            container.add(contentPanel);
        }
        SwingUtilities.invokeLater(() -> actionLogSplit.setDividerLocation(0.75));
        updateMacroStatus();
    }

    public void updateMacroStatus() {
        final IMacroSystem macros = controller.getMatchUI().getGameController() == null
                ? null : controller.getMatchUI().getGameController().macros();
        if (macros == null) {
            setStatus(localizer.getMessage("lblMacroIdle"), localizer.getMessage("lblMacroNoActionsRecorded"));
            setActions(List.of(), -1);
            setMessages(List.of());
            return;
        }

        final String playbackText = macros.playbackText();
        final List<String> actionDescriptions = macros.getRememberedActionDescriptions();
        if (macros.isRecording()) {
            setStatus(localizer.getMessage("lblMacroRecordingStatus"),
                    localizer.getMessage("lblMacroActionCount", actionDescriptions.size()));
        } else if (macros.isReplaying() || playbackText != null) {
            setStatus(localizer.getMessage("lblMacroPlaybackStatus"),
                    playbackText == null ? "" : playbackText);
        } else if (macros.hasRememberedActions()) {
            setStatus(localizer.getMessage("lblMacroReadyStatus"),
                    localizer.getMessage("lblMacroActionCount", actionDescriptions.size()));
        } else {
            setStatus(localizer.getMessage("lblMacroIdle"), localizer.getMessage("lblMacroNoActionsRecorded"));
        }

        setActions(actionDescriptions, macros.getActiveActionIndex());
        setMessages(macros.getPlaybackMessages());
    }

    private void setStatus(final String status, final String detail) {
        statusLabel.setText(status);
        detailLabel.setText(detail);
    }

    private void setActions(final List<String> actions, final int activeActionIndex) {
        if (displayedActions != null && displayedActions.equals(actions)
                && displayedActiveActionIndex == activeActionIndex) {
            return;
        }
        displayedActions = new ArrayList<>(actions);
        displayedActiveActionIndex = activeActionIndex;
        actionStartOffsets.clear();
        actionEndOffsets.clear();
        if (actions.isEmpty()) {
            actionText.setText(localizer.getMessage("lblMacroNoActionsRecorded"));
            actionText.getHighlighter().removeAllHighlights();
            return;
        }

        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            if (text.length() > 0) {
                text.append('\n');
            }
            actionStartOffsets.add(text.length());
            text.append(i + 1).append(". ").append(actions.get(i));
            actionEndOffsets.add(text.length());
        }
        actionText.setText(text.toString());
        highlightActiveAction(activeActionIndex);
    }

    private void highlightActiveAction(final int activeActionIndex) {
        actionText.getHighlighter().removeAllHighlights();
        if (activeActionIndex < 0 || activeActionIndex >= actionStartOffsets.size()) {
            actionText.setCaretPosition(actionText.getDocument().getLength());
            return;
        }
        try {
            final int start = actionStartOffsets.get(activeActionIndex);
            final int end = actionEndOffsets.get(activeActionIndex);
            actionText.getHighlighter().addHighlight(start, end, activeStepPainter);
            actionText.setCaretPosition(start);
        } catch (final BadLocationException ex) {
            actionText.setCaretPosition(actionText.getDocument().getLength());
        }
    }

    private void setMessages(final List<String> messages) {
        if (displayedMessages != null && displayedMessages.equals(messages)) {
            return;
        }
        displayedMessages = new ArrayList<>(messages);
        final boolean hasMessages = !messages.isEmpty();
        if (hasMessages && actionLogSplit.getBottomComponent() != logPanel) {
            actionLogSplit.setBottomComponent(logPanel);
            actionLogSplit.setDividerSize(8);
            actionLogSplit.setOneTouchExpandable(true);
            SwingUtilities.invokeLater(() -> actionLogSplit.setDividerLocation(0.75));
        } else if (!hasMessages && actionLogSplit.getBottomComponent() != emptyLogPanel) {
            actionLogSplit.setBottomComponent(emptyLogPanel);
            actionLogSplit.setDividerSize(0);
            actionLogSplit.setOneTouchExpandable(false);
        }
        logText.setText(messages.isEmpty() ? "" : String.join("\n", messages));
        logText.setCaretPosition(logText.getDocument().getLength());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_MACRO;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CMacro getLayoutControl() {
        return controller;
    }
}
