package forge.player;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.interfaces.IGuiBase;
import forge.view.CardView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.PlayerView;
import forge.view.SpellAbilityView;
import forge.view.StackItemView;

public abstract class PlayerControllerHuman extends PlayerController {

    private final IGuiBase gui;

    public PlayerControllerHuman(final Game game0, final Player p, final LobbyPlayer lp, final IGuiBase gui) {
        super(game0, p, lp);
        this.gui = gui;
    }

    public final IGuiBase getGui() {
        return this.gui;
    }

    public abstract boolean canUndoLastAction();
    public abstract boolean tryUndoLastAction();

    public abstract CardView getCardView(Card c);

    private final Function<Card, CardView> FN_GET_CARD_VIEW = new Function<Card, CardView>() {
        @Override
        public CardView apply(final Card input) {
            return getCardView(input);
        }
    };

    public final List<CardView> getCardViews(final List<Card> cards) {
        return Lists.transform(cards, FN_GET_CARD_VIEW);
    }
    public final Iterable<CardView> getCardViews(final Iterable<Card> cards) {
        return Iterables.transform(cards, FN_GET_CARD_VIEW);
    }
    public abstract Card getCard(CardView c);

    private final Function<CardView, Card> FN_GET_CARD = new Function<CardView, Card>() {
        @Override
        public Card apply(final CardView input) {
            return getCard(input);
        }
    };

    protected final List<Card> getCards(final List<CardView> cards) {
        return Lists.transform(cards, FN_GET_CARD);
    }

    public final GameEntityView getGameEntityView(final GameEntity e) {
        if (e instanceof Card) {
            return getCardView((Card)e);
        } else if (e instanceof Player) {
            return getPlayerView((Player)e);
        }
        return null;
    }

    public abstract PlayerView getPlayerView(Player p);

    private final Function<Player, PlayerView> FN_GET_PLAYER_VIEW = new Function<Player, PlayerView>() {
        @Override
        public PlayerView apply(final Player input) {
            return getPlayerView(input);
        }
    };

    public final List<PlayerView> getPlayerViews(final List<Player> players) {
        return Lists.transform(players, FN_GET_PLAYER_VIEW);
    }

    public abstract Player getPlayer(PlayerView p);

    public abstract SpellAbilityView getSpellAbilityView(SpellAbility sa);

    private final Function<SpellAbility, SpellAbilityView> FN_GET_SPAB_VIEW = new Function<SpellAbility, SpellAbilityView>() {
        @Override
        public SpellAbilityView apply(final SpellAbility input) {
            return getSpellAbilityView(input);
        }
    };

    public final List<SpellAbilityView> getSpellAbilityViews(final List<SpellAbility> cards) {
        return Lists.transform(cards, FN_GET_SPAB_VIEW);
    }

    public abstract SpellAbility getSpellAbility(SpellAbilityView c);

    private final Function<SpellAbilityView, SpellAbility> FN_GET_SPAB = new Function<SpellAbilityView, SpellAbility>() {
        @Override
        public SpellAbility apply(final SpellAbilityView input) {
            return getSpellAbility(input);
        }
    };

    public final List<SpellAbility> getSpellAbilities(final List<SpellAbilityView> cards) {
        return Lists.transform(cards, FN_GET_SPAB);
    }

    public final CombatView getCombat() {
        return getCombat(game.getCombat());
    }
    public abstract CombatView getCombat(Combat c);

    public abstract StackItemView getStackItemView(SpellAbilityStackInstance si);
    public abstract SpellAbilityStackInstance getStackItem(StackItemView view);
}
