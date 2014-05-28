package forge.screens.home.quest;

import forge.FThreads;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.card.CardEdition;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.*;
import forge.quest.bazaar.QuestItemType;
import forge.quest.bazaar.QuestPetController;
import forge.quest.data.QuestAchievements;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestPreferences.QPref;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuestCardShop;
import forge.screens.match.CMatchUI;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;

import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 
 * Utilities for the quest submenu, all over the MVC spectrum.
 * If a piece of code can be reused, it's dumped here.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SSubmenuQuestUtil {
    private static QuestEvent event;

    /**
     * <p>
     * nextChallengeInWins.
     * </p>
     * 
     * @return a int.
     */
    public static int nextChallengeInWins() {
        final QuestController qData = FModel.getQuest();
        final int challengesPlayed = qData.getAchievements().getChallengesPlayed();

        final int wins = qData.getAchievements().getWin();
        final int turnsToUnlock = FModel.getQuest().getTurnsToUnlockChallenge();
        final int delta;

        // First challenge unlocks after minimum wins reached.
        if (wins < 2 * turnsToUnlock) {
            delta = 2 * turnsToUnlock - wins;
        }
        else {
            // More than enough wins
            if (wins / turnsToUnlock > challengesPlayed) {
                delta = 0;
            }
            // This part takes the "unlimited challenge" bug into account;
            // a player could have an inflated challengesPlayed value.
            // Added 09-2012, can be removed after a while.
            else if (wins < challengesPlayed * turnsToUnlock) {
                delta = (challengesPlayed * turnsToUnlock - wins) + turnsToUnlock;
            }
            // Data OK, but not enough wins yet (default).
            else {
                delta = turnsToUnlock - wins % turnsToUnlock;
            }
        }

        return (delta > 0) ? delta : 0;
    }

    private static void updatePlantAndPetForView(final IVQuestStats view, final QuestController qCtrl) {
        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {
            final List<QuestPetController> petList = qCtrl.getPetsStorage().getAvaliablePets(iSlot, qCtrl.getAssets());
            final String currentPetName = qCtrl.getSelectedPet(iSlot);

            if (iSlot == 0) { // Plant visiblity
                if (petList.isEmpty()) {
                    view.getCbPlant().setVisible(false);
                }
                else {
                    view.getCbPlant().setVisible(true);
                    view.getCbPlant().setSelected(currentPetName != null);
                }
            }
            if (iSlot == 1) {
                view.getCbxPet().removeAllItems();

                // Pet list visibility
                if (petList.size() > 0) {
                    view.getCbxPet().setVisible(true);
                    view.getCbxPet().addItem("Don't summon a pet");

                    for (final QuestPetController pet : petList) {
                        String name = "Summon " + pet.getName();
                        view.getCbxPet().addItem(name);
                        if (pet.getName().equals(currentPetName)) {
                            view.getCbxPet().setSelectedItem(name);
                        }
                    }
                } else {
                    view.getCbxPet().setVisible(false);
                }
            }
        }
        
        if(qCtrl.getAssets().hasItem(QuestItemType.CHARM)) {
        	view.getCbCharm().setVisible(true);
        } else {
        	view.getCbCharm().setVisible(false);
        }

        if (view.equals(VSubmenuChallenges.SINGLETON_INSTANCE)) {
            view.getLblZep().setVisible(qCtrl.getAssets().hasItem(QuestItemType.ZEPPELIN));
            if (qCtrl.getAssets().getItemLevel(QuestItemType.ZEPPELIN) == 2) {
                view.getLblZep().setEnabled(false);
                view.getLblZep().setForeground(Color.gray);
            }
            else {
                view.getLblZep().setEnabled(true);
                view.getLblZep().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
        }
        else {
            view.getLblZep().setVisible(false);
        }
    }

    /**
     * Updates all quest info in a view, using
     * retrieval methods dictated in IVQuestStats.<br>
     * - Stats<br>
     * - Pets<br>
     * - Current deck info<br>
     * - "Challenge In" info<br>
     * 
     * @param view0 {@link forge.screens.home.quest.IVQuestStats}
     */
    public static void updateQuestView(final IVQuestStats view0) {
        final QuestController qCtrl = FModel.getQuest();
        final QuestAchievements qA = qCtrl.getAchievements();
        final QuestAssets qS = qCtrl.getAssets();

        if (qA == null) { return; }

        // Fantasy UI display
        view0.getLblNextChallengeInWins().setVisible(true);
        view0.getBtnBazaar().setVisible(true);
        view0.getLblLife().setVisible(true);

        // Stats panel
        view0.getLblCredits().setText("Credits: " + qS.getCredits());
        view0.getLblLife().setText("Life: " + qS.getLife(qCtrl.getMode()));
        view0.getLblWins().setText("Wins: " + qA.getWin());
        view0.getLblLosses().setText("Losses: " + qA.getLost());
        view0.getLblWorld().setText("World: " + (qCtrl.getWorld() == null ? "(none)" : qCtrl.getWorld()));

        // Show or hide the set unlocking button

        view0.getBtnUnlock().setVisible(qCtrl.getUnlocksTokens() > 0 && qCtrl.getWorldFormat() == null);

        // Challenge in wins
        final int num = SSubmenuQuestUtil.nextChallengeInWins();
        final String str;
        if (num == 0) {
            str = "Your exploits have been noticed. An opponent has challenged you.";
        }
        else if (num == 1) {
            str = "A new challenge will be available after 1 more win.";
        }
        else {
            str = "A new challenge will be available in " + num + " wins.";
        }

        view0.getLblNextChallengeInWins().setText(str);

        view0.getLblWinStreak().setText(
                "<html>Win streak: " + qA.getWinStreakCurrent()
                + "<br>&nbsp; (Best: " + qA.getWinStreakBest() + ")</html>");

        // Current deck message
        final SkinnedLabel lblCurrentDeck = view0.getLblCurrentDeck();
        if (SSubmenuQuestUtil.getCurrentDeck() == null) {
            lblCurrentDeck.setForeground(Color.red.darker());
            lblCurrentDeck.setText("Build, then select a deck in the \"Quest Decks\" submenu.");
        }
        else {
            lblCurrentDeck.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            lblCurrentDeck.setText("Your current deck is \""
                    + SSubmenuQuestUtil.getCurrentDeck().getName() + "\".");
        }

        // Start panel: pet, plant, zep.
        if (qCtrl.getMode() == QuestMode.Fantasy) {
            updatePlantAndPetForView(view0, qCtrl);
        }
        else {
            // Classic mode display changes
            view0.getCbxPet().setVisible(false);
            view0.getCbPlant().setVisible(false);
            view0.getCbCharm().setVisible(false);
            view0.getLblZep().setVisible(false);
            view0.getLblNextChallengeInWins().setVisible(false);
            view0.getBtnBazaar().setVisible(false);
            view0.getLblLife().setVisible(false);
        }
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getCurrentDeck() {
        Deck d = null;

        if (FModel.getQuest().getAssets() != null) {
            d = FModel.getQuest().getMyDecks().get(
                FModel.getQuestPreferences().getPref(QPref.CURRENT_DECK));
        }

        return d;
    }

    /** Updates the current selected quest event, used when game is started.
     * @param event0 {@link forge.quest.QuestEvent}
     */
    public static void setEvent(final QuestEvent event0) {
        SSubmenuQuestUtil.event = event0;
    }

    public static boolean checkActiveQuest(String location) {
        QuestController qc = FModel.getQuest();
        if (qc == null || qc.getAssets() == null) {
            String msg = "Please create a Quest before attempting to " + location;
            FOptionPane.showErrorDialog(msg, "No Quest");
            System.out.println(msg);
            return false;
        }
        return true;
    }
    
    /** */
    public static void showSpellShop() {
        if (!checkActiveQuest("Visit the Spell Shop.")) {
            return;
        }
        Singletons.getControl().setCurrentScreen(FScreen.QUEST_CARD_SHOP);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                new CEditorQuestCardShop(FModel.getQuest()));
    }

    /** */
    public static void showBazaar() {
        if (!checkActiveQuest("Visit the Bazaar.")) {
            return;
        }
        Singletons.getControl().setCurrentScreen(FScreen.QUEST_BAZAAR);
        Singletons.getView().getFrame().validate();
    }

    /** */
    public static void chooseAndUnlockEdition() {
        if (!checkActiveQuest("Unlock Editions.")) {
            return;
        }
        final QuestController qData = FModel.getQuest();
        ImmutablePair<CardEdition, Integer> toUnlock = QuestUtilUnlockSets.chooseSetToUnlock(qData, false, null);
        if (toUnlock == null) {
            return;
        }

        CardEdition unlocked = toUnlock.left;
        qData.getAssets().subtractCredits(toUnlock.right);
        FOptionPane.showMessageDialog("You have successfully unlocked " + unlocked.getName() + "!",
                unlocked.getName() + " unlocked!", null);

        QuestUtilUnlockSets.doUnlock(qData, unlocked);
    }

    /** */
    public static void travelWorld() {
        if (!checkActiveQuest("Travel between worlds.")) {
            return;
        }
        List<QuestWorld> worlds = new ArrayList<QuestWorld>();
        final QuestController qCtrl = FModel.getQuest();

        for (QuestWorld qw : FModel.getWorlds()) {
            if (qCtrl.getWorld() != qw) {
                worlds.add(qw);
            }
        }

        if (worlds.size() < 1) {
            FOptionPane.showErrorDialog("There are currently no worlds you can travel to\nin this version of Forge.", "No Worlds");
            return;
        }

        final String setPrompt = "Where do you wish to travel?";
        final QuestWorld newWorld = GuiChoose.oneOrNone(setPrompt, worlds);

        if (worlds.indexOf(newWorld) < 0) {
            return;
        }

        if (qCtrl.getWorld() != newWorld) {
            boolean needRemove = false;
            if (nextChallengeInWins() < 1 && qCtrl.getAchievements().getCurrentChallenges().size() > 0) {
                needRemove = true;

                if (!FOptionPane.showConfirmDialog(
                        "You have uncompleted challenges in your current world. If you travel now, they will be LOST!"
                        + "\nAre you sure you wish to travel anyway?\n"
                        + "(Click \"No\" to go back  and complete your current challenges first.)",
                        "WARNING: Uncompleted challenges")) {
                    return;
                }
            }

            if (needRemove) {
                // Remove current challenges.
                while (nextChallengeInWins() == 0) {
                    qCtrl.getAchievements().addChallengesPlayed();
                }

                qCtrl.getAchievements().getCurrentChallenges().clear();
            }

            qCtrl.setWorld(newWorld);
            qCtrl.resetDuelsManager();
            qCtrl.resetChallengesManager();
            // Note that the following can be (ab)used to re-randomize your opponents by travelling to a different
            // world and back. To prevent this, simply delete the following line that randomizes DuelsManager.
            // (OTOH, you can 'swap' opponents even more easily  by simply selecting a different quest data file and
            // then re-selecting your current quest data file.)
            qCtrl.getDuelsManager().randomizeOpponents();
            qCtrl.getCards().clearShopList();
            qCtrl.save();
        }
    }

    /** */
    public static void startGame() {
        if (!checkActiveQuest("Start a duel.") || null == event) {
            return;
        }
        final QuestController qData = FModel.getQuest();

        Deck deck = null;
        if (event instanceof QuestEventChallenge) {
            // Predefined HumanDeck
            deck = ((QuestEventChallenge) event).getHumanDeck();
        }
        if (deck == null) {
            // If no predefined Deck, use the Player's Deck
            deck = SSubmenuQuestUtil.getCurrentDeck();
        }
        if (deck == null) {
            String msg = "Please select a Quest Deck.";
            FOptionPane.showErrorDialog(msg, "No Deck");
            System.out.println(msg);
            return;
        }
        
        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = GameType.Quest.getDecksFormat().getDeckConformanceProblem(deck);
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage +  " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() {
                qData.getDuelsManager().randomizeOpponents();
                qData.setCurrentEvent(event);
                qData.save();

                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
        
        int extraLifeHuman = 0;
        Integer lifeHuman = null;
        boolean useBazaar = true;
        Boolean forceAnte = null;
        int lifeAI = 20;
        if (event instanceof QuestEventChallenge) {
            QuestEventChallenge qc = ((QuestEventChallenge) event);
            lifeAI = qc.getAILife();
            lifeHuman = qc.getHumanLife();

            if (qData.getAssets().hasItem(QuestItemType.ZEPPELIN)) {
                extraLifeHuman = 3;
            }

            useBazaar = qc.isUseBazaar();
            forceAnte = qc.isForceAnte();
        }

        RegisteredPlayer humanStart = new RegisteredPlayer(deck);
        RegisteredPlayer aiStart = new RegisteredPlayer(event.getEventDeck());
        
        if (lifeHuman != null) {
            humanStart.setStartingLife(lifeHuman);
        } else {
            humanStart.setStartingLife(qData.getAssets().getLife(qData.getMode()) + extraLifeHuman);
        }

        if (useBazaar) {
            humanStart.setCardsOnBattlefield(QuestUtil.getHumanStartingCards(qData, event));
            aiStart.setStartingLife(lifeAI);  
            aiStart.setCardsOnBattlefield(QuestUtil.getComputerStartingCards(event));
        }

        List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        starter.add(humanStart.setPlayer(GuiBase.getInterface().getQuestPlayer()));

        LobbyPlayer aiPlayer = FControl.instance.getAiPlayer(event.getOpponent() == null ? event.getTitle() : event.getOpponent());
        CMatchUI.SINGLETON_INSTANCE.avatarImages.put(aiPlayer, event.getIconImageKey());
        starter.add(aiStart.setPlayer(aiPlayer));

        boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
        for(RegisteredPlayer rp : starter) {
            rp.setRandomFoil(useRandomFoil);
        }
        boolean useAnte = FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE);
        boolean matchAnteRarity = FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE_MATCH_RARITY);
        if(forceAnte != null)
            useAnte = forceAnte.booleanValue();
        GameRules rules = new GameRules(GameType.Quest);
        rules.setPlayForAnte(useAnte);
        rules.setMatchAnteRarity(matchAnteRarity);
        rules.setGamesPerMatch(qData.getCharmState() ? 5 : 3);
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.canCloneUseTargetsImage = FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);
        final Match mc = new Match(rules, starter);
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                Singletons.getControl().startGameWithUi(mc);
                // no overlays here?
            }
        });
    }
    
    /**
     * Checks to see if a game can be started and displays relevant dialogues.
     * @return
     */
    public static boolean canStartGame() {
        
        if (!checkActiveQuest("Start a duel.") || null == event) {
            return false;
        }
        
        Deck deck = null;
        if (event instanceof QuestEventChallenge) {
            // Predefined HumanDeck
            deck = ((QuestEventChallenge) event).getHumanDeck();
        }
        
        if (deck == null) {
            // If no predefined Deck, use the Player's Deck
            deck = SSubmenuQuestUtil.getCurrentDeck();
        }
        
        if (deck == null) {
            String msg = "Please select a Quest Deck.";
            FOptionPane.showErrorDialog(msg, "No Deck");
            System.out.println(msg);
            return false;
        }
        
        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = GameType.Quest.getDecksFormat().getDeckConformanceProblem(deck);
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage +  " Please edit or choose a different deck.", "Invalid Deck");
                return false;
            }
        }
        
        return true;
        
    }

    /** Duplicate in DeckEditorQuestMenu and
     * probably elsewhere...can streamline at some point
     * (probably shouldn't be here).
     * 
     * @param in &emsp; {@link java.lang.String}
     * @return {@link java.lang.String}
     */
    public static String cleanString(final String in) {
        final StringBuffer out = new StringBuffer();
        final char[] c = in.toCharArray();

        for (int i = 0; (i < c.length) && (i < 20); i++) {
            if (Character.isLetterOrDigit(c[i]) || (c[i] == '-') || (c[i] == '_') || (c[i] == ' ')) {
                out.append(c[i]);
            }
        }

        return out.toString();
    }
}

