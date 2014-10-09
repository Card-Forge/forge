package forge.game;

import java.util.List;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.trackable.TrackableIndex;

public class GameView {
    private final TrackableIndex<CardView> cards = new TrackableIndex<CardView>();
    private final TrackableIndex<PlayerView> players = new TrackableIndex<PlayerView>();
    private final TrackableIndex<SpellAbilityView> spellAbilities = new TrackableIndex<SpellAbilityView>();
    private final TrackableIndex<StackItemView> stackItems = new TrackableIndex<StackItemView>();
    private CombatView combatView;

    public GameView() {
        
    }

    public CombatView getCombatView() {
        return combatView;
    }

    public void refreshCombat(Game game) {
        final Combat combat = game.getCombat();
        if (combat == null) {
            combatView = null;
            return;
        }

        combatView = new CombatView();
        for (final AttackingBand b : combat.getAttackingBands()) {
            if (b == null) continue;
            final GameEntity defender = combat.getDefenderByAttacker(b);
            final List<Card> blockers = combat.getBlockers(b);
            final boolean isBlocked = b.isBlocked() == Boolean.TRUE;
            combatView.addAttackingBand(
                    CardView.getCollection(b.getAttackers()),
                    GameEntityView.get(defender),
                    isBlocked ? CardView.getCollection(blockers) : null,
                    CardView.getCollection(blockers));
        }
    }

    public void serialize() {
        /*try {
            GameStateSerializer serializer = new GameStateSerializer(filename);
            game.saveState(serializer);
            serializer.writeEndOfFile();
            serializer.bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public void deserialize() {
        /*GameStateDeserializer deserializer = new GameStateDeserializer();
        deserializer.readObject();*/
    }
}
