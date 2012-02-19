package forge.view.home;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FList;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.WrapLayout;

/** 
 * Shows and manages list selectors for each type of deck
 * possible in constructed mode.  This panel is fully self-contained,
 * there should be no need for further control.
 * 
 * <br><br>The current selected deck can be retrieved using getLstCurrentSelected().
 */
@SuppressWarnings("serial")
public class ConstructedDeckSelectPanel extends JPanel {
    private final JList lstCustomDecks, lstQuestDecks, lstColorDecks, lstThemeDecks;
    private final String multi = System.getProperty("os.name").equals("Mac OS X") ? "CMD" : "CTRL";
    private final String colorsTT = "Generate deck (Multi-select: " + multi + ")";
    private final String themeTT = "Generate deck with a theme";
    private final String userTT = "Custom decks (Decklist: Double Click)";
    private final String questTT = "Quest event decks (Decklist: Double Click)";
    private final FLabel btnRandomTheme, btnRandomCustom, btnRandomQuest;
    private final MouseListener madDeckList;
    private JList lstCurrentSelected;

    /**
     * 
     * Basic container for deck choices available in constructed mode.
     * Can (should, will be) applied to multiple AIs or humans.
     * @param c0
     */
    public ConstructedDeckSelectPanel() {
        this.setLayout(new WrapLayout(FlowLayout.CENTER, 10, 10));
        this.setOpaque(false);
        final Dimension size = new Dimension(170, 250);

        final Command cmdRandomTheme = new Command() {  @Override
            public void execute() { randomDeckPick(lstThemeDecks); } };
        final Command cmdRandomUser = new Command() {  @Override
            public void execute() { randomDeckPick(lstCustomDecks); } };
        final Command cmdRandomQuest = new Command() {  @Override
            public void execute() { randomDeckPick(lstQuestDecks); } };

        btnRandomTheme = new FLabel.Builder().text("Random").fontScaleAuto(false)
                .hoverable(true).cmdClick(cmdRandomTheme).opaque(true).build();

        btnRandomCustom = new FLabel.Builder().text("Random").fontScaleAuto(false)
                .hoverable(true).cmdClick(cmdRandomUser).opaque(true).build();

        btnRandomQuest = new FLabel.Builder().text("Random").fontScaleAuto(false)
                .hoverable(true).cmdClick(cmdRandomQuest).opaque(true).build();

        lstCustomDecks = new FList("lstCustom");
        lstQuestDecks = new FList("lstQuest");
        lstColorDecks = new FList("lstColor");
        lstThemeDecks = new FList("lstTheme");

        lstCustomDecks.setToolTipText(userTT);
        lstQuestDecks.setToolTipText(questTT);
        lstColorDecks.setToolTipText(colorsTT);
        lstThemeDecks.setToolTipText(themeTT);

        lstColorDecks.setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        lstCustomDecks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstQuestDecks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstThemeDecks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JPanel pnlCustom = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        final FScrollPane scrCustom = new FScrollPane(lstCustomDecks);
        scrCustom.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrCustom.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        pnlCustom.setOpaque(false);
        pnlCustom.add(scrCustom, "w 100%!, h 89%!");
        pnlCustom.add(btnRandomCustom, "w 100%!, h 9%!, gap 0 0 1% 1%, ax center");

        final JPanel pnlQuest = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        final FScrollPane scrQuest = new FScrollPane(lstQuestDecks);
        scrQuest.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrQuest.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        pnlQuest.setOpaque(false);
        pnlQuest.add(scrQuest, "w 100%!, h 89%!");
        pnlQuest.add(btnRandomQuest, "w 100%!, h 9%!, gap 0 0 1% 1%, ax center");

        final JPanel pnlTheme = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        final FScrollPane scrTheme = new FScrollPane(lstThemeDecks);
        scrTheme.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrTheme.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        pnlTheme.setOpaque(false);
        pnlTheme.add(scrTheme, "w 100%!, h 89%!");
        pnlTheme.add(btnRandomTheme, "w 100%!, h 9%!, gap 0 0 1% 1%, ax center");

        // Sizing and adding
        pnlCustom.setMinimumSize(size);
        pnlCustom.setMaximumSize(size);
        pnlCustom.setPreferredSize(size);

        pnlQuest.setMinimumSize(size);
        pnlQuest.setMaximumSize(size);
        pnlQuest.setPreferredSize(size);

        lstColorDecks.setMinimumSize(size);
        lstColorDecks.setMaximumSize(size);
        lstColorDecks.setPreferredSize(size);

        pnlTheme.setMinimumSize(size);
        pnlTheme.setMaximumSize(size);
        pnlTheme.setPreferredSize(size);

        this.add(lstColorDecks);
        this.add(pnlCustom);
        this.add(pnlTheme);
        this.add(pnlQuest);

        // Listener init
        madDeckList = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listSelectManager((JList) e.getSource());

                if (e.getClickCount() == 2
                       && (((JList) e.getSource()).getName().equals("lstQuest") || ((JList) e.getSource()).getName().equals("lstCustom"))) {
                    final String deckName = ((JList) e.getSource()).getSelectedValue().toString();
                    showDecklist(AllZone.getDecks().getConstructed().get(deckName));
                }
            }
        };

        lstColorDecks.removeMouseListener(madDeckList);
        lstColorDecks.addMouseListener(madDeckList);

        lstThemeDecks.removeMouseListener(madDeckList);
        lstThemeDecks.addMouseListener(madDeckList);

        lstQuestDecks.removeMouseListener(madDeckList);
        lstQuestDecks.addMouseListener(madDeckList);

        lstCustomDecks.removeMouseListener(madDeckList);
        lstCustomDecks.addMouseListener(madDeckList);
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstCustomDecks() {
        return this.lstCustomDecks;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstQuestDecks() {
        return this.lstQuestDecks;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstColorDecks() {
        return this.lstColorDecks;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstThemeDecks() {
        return this.lstThemeDecks;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstCurrentSelected() {
        return this.lstCurrentSelected;
    }

    /**
     * Prevents decks from different lists being selected simultaneously.
     * Also, sets lstCurrentSelected, for use when the game is started.
     * 
     * @param src0 &emsp; {@link javax.swing.JList}
     */
    public void listSelectManager(final JList src0) {
        if (src0.getSelectedValues().length == 0) { return; }

        // Clear any previous selection.
        if (src0 != lstCustomDecks) { lstCustomDecks.clearSelection(); }
        if (src0 != lstQuestDecks) { lstQuestDecks.clearSelection(); }
        if (src0 != lstColorDecks) { lstColorDecks.clearSelection(); }
        if (src0 != lstThemeDecks) { lstThemeDecks.clearSelection(); }

        lstCurrentSelected = src0;
    }

    /** Shows decklist dialog for a given deck. */
    private void showDecklist(Deck d0) {
        HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

        for (Entry<CardPrinted, Integer> s : d0.getMain()) {
            deckMap.put(s.getKey().getName(), s.getValue());
        }

        String nl = System.getProperty("line.separator");
        StringBuilder deckList = new StringBuilder();
        String dName = d0.getName();

        deckList.append(dName == null ? "" : dName + nl + nl);

        ArrayList<String> dmKeys = new ArrayList<String>();
        for (final String s : deckMap.keySet()) {
            dmKeys.add(s);
        }

        Collections.sort(dmKeys);

        for (String s : dmKeys) {
            deckList.append(deckMap.get(s) + " x " + s + nl);
        }

        final StringBuilder msg = new StringBuilder();
        if (deckMap.keySet().size() <= 32) {
            msg.append(deckList.toString() + nl);
        } else {
            msg.append("Decklist too long for dialog." + nl + nl);
        }

        msg.append("Copy Decklist to Clipboard?");

        // Output
        int rcMsg = JOptionPane.showConfirmDialog(null, msg, "Decklist", JOptionPane.OK_CANCEL_OPTION);
        if (rcMsg == JOptionPane.OK_OPTION) {
            final StringSelection ss = new StringSelection(deckList.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        }
    } // End showDecklist

    /**
     * Random chooser for theme decks.
     * 
     * @param lst0 {@link javax.swing.JList}
     */
    private void randomDeckPick(JList lst0) {
        final int size = lst0.getModel().getSize();

        if (size > 0) {
            final Random r = new Random();
            final int i = r.nextInt(size);

            lst0.setSelectedIndex(i);
            lst0.ensureIndexIsVisible(lst0.getSelectedIndex());
        }

        listSelectManager(lst0);
    }
}
