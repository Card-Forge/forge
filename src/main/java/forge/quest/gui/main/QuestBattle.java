package forge.quest.gui.main;


import forge.deck.Deck;
import forge.gui.GuiUtils;
import forge.quest.data.QuestBattleManager;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>QuestBattle class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestBattle extends QuestSelectablePanel {
    /** Constant <code>serialVersionUID=3112668476017792084L</code> */
    private static final long serialVersionUID = 3112668476017792084L;

    String deckName;

    /**
     * <p>getBattles.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<QuestSelectablePanel> getBattles() {
        List<QuestSelectablePanel> opponentList = new ArrayList<QuestSelectablePanel>();

        String[] oppDecks = QuestBattleManager.getOpponents();
        for (String oppDeckName : oppDecks) {
            // Get deck object, its properties, and icon for this opponent.
            Deck oppDeck    = QuestBattleManager.getAIDeckNewFormat(oppDeckName);
            
            String oppName  = oppDeckName.substring(0, oppDeckName.length() - 1).trim();
            String oppDiff  = oppDeck.getMetadata("Difficulty");
            String oppDesc  = oppDeck.getMetadata("Description");
            ImageIcon icon  = GuiUtils.getIconFromFile(oppName + ".jpg");
            
            // SHOULD BE HANDLED IN getMetadata(), will be soon enough.
            if(oppDiff.equals("")) { System.out.println(oppDeckName+" missing deck difficulty."); oppName = "<<Unknown>>"; }
            if(oppDesc.equals("")) { System.out.println(oppDeckName+" missing deck description."); oppName = "<<Unknown>>"; }
            
            // Add to list of current quest opponents.
            opponentList.add(
                    new QuestBattle(oppDeckName, oppDiff, oppDesc, icon)
            );
        }

        return opponentList;
    }

    /**
     * <p>Constructor for QuestBattle.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param difficulty a {@link java.lang.String} object.
     * @param description a {@link java.lang.String} object.
     * @param icon a {@link javax.swing.ImageIcon} object.
     */
    private QuestBattle(String name, String difficulty, String description, ImageIcon icon) {
        super(name.substring(0, name.length() - 2), difficulty, description, icon);

        this.deckName = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return deckName;
    }
}
