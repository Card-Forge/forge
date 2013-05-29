package forge.game.event;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IGameEventVisitor<T> {
    T visit(GameEventBlockerAssigned event);
    T visit(GameEventCardDamaged event);
    T visit(GameEventCardDestroyed event);
    T visit(GameEventCardDiscarded event);
    T visit(GameEventCardEquipped event);
    T visit(GameEventCardRegenerated event);
    T visit(GameEventCardSacrificed event);
    T visit(GameEventAnteCardsSelected event);
    T visit(GameEventCardTapped event);
    T visit(GameEventCounterAdded event);
    T visit(GameEventCounterRemoved event);
    T visit(GameEventDrawCard event);
    T visit(GameEventDuelFinished event);
    T visit(GameEventDuelOutcome event);
    T visit(GameEventEndOfTurn event);
    T visit(GameEventFlipCoin event);
    T visit(GameEventGameRestarted event);
    T visit(GameEventLandPlayed event);
    T visit(GameEventLifeLoss event);
    T visit(GameEventManaBurn event);
    T visit(GameEventMulligan event);
    T visit(GameEventPlayerControl event);
    T visit(GameEventPoisonCounter event);
    T visit(GameEventShuffle event);
    T visit(GameEventSpellResolved event);
    T visit(GameEventTokenCreated event);
    T visit(GameEventTurnPhase event);
    
    
    // This is base class for all visitors.
    public static class Base<T> implements IGameEventVisitor<T>{
        public T visit(GameEventBlockerAssigned event) { return null; }
        public T visit(GameEventCardDamaged event) { return null; }
        public T visit(GameEventCardDestroyed event) { return null; }
        public T visit(GameEventCardDiscarded event) { return null; }
        public T visit(GameEventCardEquipped event) { return null; }
        public T visit(GameEventCardRegenerated event) { return null; }
        public T visit(GameEventCardSacrificed event) { return null; }
        public T visit(GameEventAnteCardsSelected event) { return null; }
        public T visit(GameEventCardTapped event) { return null; }
        public T visit(GameEventCounterAdded event) { return null; }
        public T visit(GameEventCounterRemoved event) { return null; }
        public T visit(GameEventDrawCard event) { return null; }
        public T visit(GameEventDuelFinished event) { return null; }
        public T visit(GameEventDuelOutcome event) { return null; }
        public T visit(GameEventEndOfTurn event) { return null; }
        public T visit(GameEventFlipCoin event) { return null; }
        public T visit(GameEventGameRestarted event) { return null; }
        public T visit(GameEventLandPlayed event) { return null; }
        public T visit(GameEventLifeLoss event) { return null; }
        public T visit(GameEventManaBurn event) { return null; }
        public T visit(GameEventMulligan event) { return null; }
        public T visit(GameEventPlayerControl event) { return null; }
        public T visit(GameEventPoisonCounter event) { return null; }
        public T visit(GameEventShuffle event) { return null; }
        public T visit(GameEventSpellResolved event) { return null; }
        public T visit(GameEventTokenCreated event) { return null; }
        public T visit(GameEventTurnPhase event) { return null; }
    }
}

