package forge.game.event;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IGameEventVisitor<T,U> {
    U visit(GameEventBlockerAssigned event, T params);
    U visit(GameEventCardDamaged event, T params);
    U visit(GameEventCardDestroyed event, T params);
    U visit(GameEventCardDiscarded event, T params);
    U visit(GameEventCardEquipped event, T params);
    U visit(GameEventCardRegenerated event, T params);
    U visit(GameEventCardSacrificed event, T params);
    U visit(GameEventAnteCardsSelected event, T params);
    U visit(GameEventCardTapped event, T params);
    U visit(GameEventCounterAdded event, T params);
    U visit(GameEventCounterRemoved event, T params);
    U visit(GameEventDrawCard event, T params);
    U visit(GameEventDuelFinished event, T params);
    U visit(GameEventDuelOutcome event, T params);
    U visit(GameEventEndOfTurn event, T params);
    U visit(GameEventFlipCoin event, T params);
    U visit(GameEventGameRestarted event, T params);
    U visit(GameEventLandPlayed event, T params);
    U visit(GameEventLifeLoss event, T params);
    U visit(GameEventManaBurn event, T params);
    U visit(GameEventMulligan event, T params);
    U visit(GameEventPlayerControl event, T params);
    U visit(GameEventPoisonCounter event, T params);
    U visit(GameEventShuffle event, T params);
    U visit(GameEventSpellResolved event, T params);
    U visit(GameEventTokenCreated event, T params);
    U visit(GameEventTurnPhase event, T params);
    
    
    // This is base class for all visitors.
    public static class Base<T,U> implements IGameEventVisitor<T,U>{
        public U visit(GameEventBlockerAssigned event, T params) { return null; }
        public U visit(GameEventCardDamaged event, T params) { return null; }
        public U visit(GameEventCardDestroyed event, T params) { return null; }
        public U visit(GameEventCardDiscarded event, T params) { return null; }
        public U visit(GameEventCardEquipped event, T params) { return null; }
        public U visit(GameEventCardRegenerated event, T params) { return null; }
        public U visit(GameEventCardSacrificed event, T params) { return null; }
        public U visit(GameEventAnteCardsSelected event, T params) { return null; }
        public U visit(GameEventCardTapped event, T params) { return null; }
        public U visit(GameEventCounterAdded event, T params) { return null; }
        public U visit(GameEventCounterRemoved event, T params) { return null; }
        public U visit(GameEventDrawCard event, T params) { return null; }
        public U visit(GameEventDuelFinished event, T params) { return null; }
        public U visit(GameEventDuelOutcome event, T params) { return null; }
        public U visit(GameEventEndOfTurn event, T params) { return null; }
        public U visit(GameEventFlipCoin event, T params) { return null; }
        public U visit(GameEventGameRestarted event, T params) { return null; }
        public U visit(GameEventLandPlayed event, T params) { return null; }
        public U visit(GameEventLifeLoss event, T params) { return null; }
        public U visit(GameEventManaBurn event, T params) { return null; }
        public U visit(GameEventMulligan event, T params) { return null; }
        public U visit(GameEventPlayerControl event, T params) { return null; }
        public U visit(GameEventPoisonCounter event, T params) { return null; }
        public U visit(GameEventShuffle event, T params) { return null; }
        public U visit(GameEventSpellResolved event, T params) { return null; }
        public U visit(GameEventTokenCreated event, T params) { return null; }
        public U visit(GameEventTurnPhase event, T params) { return null; }
    }
}

