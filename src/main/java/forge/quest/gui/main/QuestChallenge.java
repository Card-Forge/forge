package forge.quest.gui.main;

import forge.CardList;
import forge.item.CardPrinted;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>QuestQuest class.</p>
 * 
 * MODEL - A single quest event data instance, including meta, 
 * deck, and quest-specific properties.
 * 
 */
public class QuestChallenge extends QuestEvent {
    // ID (default -1, should be explicitly set at later time.)
    public int id = -1;
    
    // Default vals if none provided for this ID
    public int     aiLife              = 25;
    public int     creditsReward       = 100;
    public String  cardReward          = "1 colorless rare";
    public boolean repeatable          = false;
    public int     winsReqd            = 20;

    // Other cards used in assignment: starting, and reward.
    public CardList humanExtraCards    = new CardList();
    public CardList aiExtraCards       = new CardList();
    public List<CardPrinted> cardRewardList = new ArrayList<CardPrinted>(); 

    public QuestChallenge() {
        super();
        eventType = "challenge";
    }
    
    /**
     * <p>getAILife.</p>
     *
     * @return {@link java.lang.Integer}.
     */
    public final int getAILife() { 
        return aiLife; 
    }
    
    /**
     * <p>getCardReward.</p>
     *
     * @return {@link java.lang.String}.
     */
    public final String getCardReward() { 
        return cardReward; 
    }
    
    /**
     * <p>getCreditsReward.</p>
     *
     * @return {@link java.lang.Integer}.
     */
    public final int getCreditsReward() { 
        return creditsReward; 
    }
    
    /**
     * <p>getId.</p>
     *
     * @return {@link java.lang.Integer}.
     */
    public final int getId() { 
        return id; 
    }
    
    /**
     * <p>getRepeatable.</p>
     *
     * @return {@link java.lang.Boolean}.
     */
    public final boolean getRepeatable() { 
        return repeatable; 
    }
    
    /**
     * <p>getWinsReqd.</p>
     *
     * @return {@link java.lang.Integer}.
     */
    public final int getWinsReqd() { 
        return winsReqd; 
    }
    
    /**
     * <p>getAIExtraCards.</p>
     * Retrieves list of cards AI has in play at the beginning of this quest.
     *
     * @return 
     */
    public final CardList getAIExtraCards() { 
        return aiExtraCards; 
    }
    
    /**
     * <p>getHumanExtraCards.</p>
     * Retrieves list of cards human has in play at the beginning of this quest.
     *
     * @return 
     */
    public final CardList getHumanExtraCards() { 
        return humanExtraCards; 
    }
    
    /**
     * <p>getCardRewardList.</p>
     *
     * @return 
     */
    public final List<CardPrinted> getCardRewardList() { 
        return cardRewardList; 
    }
}
