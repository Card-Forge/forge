package forge.gui.input;

import java.util.ArrayList;

import forge.*;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.GamePlayerRating;
import forge.game.PlayerIndex;
import forge.quest.data.QuestData;

/**
 * <p>Input_Mulligan class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Input_Mulligan extends Input {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code> */
    private static final long serialVersionUID = -8112954303001155622L;
    
    private static final int MAGIC_NUMBER_OF_SHUFFLES = 100;
    private static final int AI_MULLIGAN_THRESHOLD = 5;

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        ButtonUtil.enableAll();
        AllZone.getDisplay().getButtonOK().setText("No");
        AllZone.getDisplay().getButtonCancel().setText("Yes");
        AllZone.getDisplay().showMessage("Do you want to Mulligan?");
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonOK() {
        end();
    }

    public int doMulligan( Player player, GamePlayerRating playerRating ) {
        CardList hand = AllZoneUtil.getPlayerHand(player);
        for (Card c : hand) { AllZone.getGameAction().moveToLibrary(c); }
        for (int i = 0; i < MAGIC_NUMBER_OF_SHUFFLES; i++) { player.shuffle(); }
        int newHand = hand.size() - 1;
        for (int i = 0; i < newHand; i++) { player.drawCard(); }
        playerRating.notifyHasMulliganed();
        playerRating.notifyOpeningHandSize(newHand);
        return newHand;
    }
    
    /** {@inheritDoc} */
    @Override
    public void selectButtonCancel() {
        GamePlayerRating humanRating = AllZone.getGameInfo().getPlayerRating(PlayerIndex.HUMAN);
        Player humanPlayer = AllZone.getHumanPlayer();

        int newHand = doMulligan(humanPlayer, humanRating);

        QuestData quest = AllZone.getQuestData();
        if (quest != null && quest.getInventory().hasItem("Sleight") && humanRating.getMulliganCount() == 1) {
            AllZone.getHumanPlayer().drawCard();
            humanRating.notifyOpeningHandSize(newHand + 1);
        }

        if (newHand == 0) {
            end();
        }
    }//selectButtonOK()

    /**
     * <p>end.</p>
     */
    void end() {
        //Computer mulligan
        Player aiPlayer = AllZone.getComputerPlayer();
        GamePlayerRating aiRating = AllZone.getGameInfo().getPlayerRating(PlayerIndex.AI);
        boolean aiTakesMulligan = true;
        
        //Computer mulligans if there are no cards with converted mana cost of 0 in its hand
        while (aiTakesMulligan) {

            CardList handList = AllZoneUtil.getPlayerHand(aiPlayer);
            boolean hasLittleCmc0Cards = handList.getValidCards("Card.cmcEQ0", aiPlayer, null).size() < 2;
            aiTakesMulligan = handList.size() > AI_MULLIGAN_THRESHOLD && hasLittleCmc0Cards;

            if (aiTakesMulligan) {
                doMulligan(aiPlayer, aiRating);
            }            
        }

        //Human Leylines & Chancellors
        ButtonUtil.reset();
        AbilityFactory af = new AbilityFactory();
        CardList humanOpeningHand = AllZoneUtil.getPlayerHand(AllZone.getHumanPlayer());
        
        for (Card c : humanOpeningHand) {
            ArrayList<String> kws = c.getKeyword(); 
            for(int i = 0;i<kws.size();i++) {
                String kw = kws.get(i);
                
                if(kw.startsWith("MayEffectFromOpeningHand"))
                {
                    String effName = kw.split(":")[1];
                    
                    SpellAbility effect = af.getAbility(c.getSVar(effName), c);
                    if(GameActionUtil.showYesNoDialog(c, "Use this card's ability?"))
                    {
                        //If we ever let the AI memorize cards in the players hand, this would be a place to do so.
                        AllZone.getGameAction().playSpellAbility_NoStack(effect, false);
                    }
                }
            }
        }

        //Computer Leylines & Chancellors
        CardList aiOpeningHand = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
        for (Card c : aiOpeningHand) {
            if (!(c.getName().startsWith("Leyline of Singularity")
                    && AllZoneUtil.getCardsInPlay("Leyline of Singularity").size() > 0)) {
                ArrayList<String> kws = c.getKeyword(); 
                for(int i = 0;i<kws.size();i++) {
                    String kw = kws.get(i);
                    
                    if(kw.startsWith("MayEffectFromOpeningHand"))
                    {
                        String effName = kw.split(":")[1];
                        
                        SpellAbility effect = af.getAbility(c.getSVar(effName), c);

                        if(effect.doTrigger(false)) //Is there a better way for the AI to decide this?
                        {
                            GameActionUtil.showInfoDialg("Computer reveals " + c.getName() + "(" + c.getUniqueNumber() + ").");
                            ComputerUtil.playNoStack(effect);
                        }
                    }
                }
            }

        }
        AllZone.getGameAction().checkStateEffects();
        
        
        if (AllZone.getGameAction().isStartCut() && !(humanOpeningHand.contains(AllZone.getGameAction().getHumanCut())
                || aiOpeningHand.contains(AllZone.getGameAction().getComputerCut()))) {
            AllZone.getGameAction().moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.getHumanPlayer()), AllZone.getGameAction().getHumanCut());
            AllZone.getGameAction().moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.getComputerPlayer()), AllZone.getGameAction().getComputerCut());
        }
        AllZone.getGameAction().checkStateEffects();
        Phase.setGameBegins(1);
        AllZone.getPhase().setNeedToNextPhase(false);
        stop();
    }
}
