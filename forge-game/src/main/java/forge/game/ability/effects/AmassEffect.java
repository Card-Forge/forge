package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class AmassEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Amass ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        sb.append(amount).append(" (Put ");

        sb.append(Lang.nounWithNumeral(amount, "+1/+1 counter"));

        sb.append("on an Army you control. If you don’t control one, create a 0/0 black Zombie Army creature token first.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();

        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);
        final boolean remember = sa.hasParam("RememberAmass");

        // create army token if needed
        if (CardLists.count(activator.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army")) == 0) {
            final String tokenScript = "b_0_0_zombie_army";

            final Card prototype = TokenInfo.getProtoType(tokenScript, sa);

            for (final Card tok : TokenInfo.makeTokensFromPrototype(prototype, activator, 1, true)) {

                // Should this be catching the Card that's returned?
                Card c = game.getAction().moveToPlay(tok, sa);
                c.updateStateForView();
            }

            game.fireEvent(new GameEventTokenCreated());
        }

        CardCollectionView tgtCards = CardLists.getType(activator.getCardsIn(ZoneType.Battlefield), "Army");
        tgtCards = pc.chooseCardsForEffect(tgtCards, sa, "Choose an army to put counters on", 1, 1, false);

        GameEntityCounterTable table = new GameEntityCounterTable();
        for(final Card tgtCard : tgtCards) {
            tgtCard.addCounter(CounterType.P1P1, amount, activator, true, table);
            game.updateLastStateForCard(tgtCard);

            if (remember) {
                card.addRemembered(tgtCard);
            }
        }
        table.triggerCountersPutAll(game);
    }

}
