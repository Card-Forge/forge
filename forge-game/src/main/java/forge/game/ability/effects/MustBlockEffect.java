package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class MustBlockEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        List<Card> cards;
        if (sa.hasParam("DefinedAttacker")) {
            cards = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedAttacker"), sa);
            if (cards.isEmpty()) {
                return;
            }
        } else {
            cards = Lists.newArrayList(host);
        }

        final List<Card> tgtCards = Lists.newArrayList();
        if (sa.hasParam("Choices")) {
            Player chooser = activator;
            if (sa.hasParam("Chooser")) {
                final String choose = sa.getParam("Chooser");
                chooser = AbilityUtils.getDefinedPlayers(host, choose, sa).get(0);
            }

            CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host, sa);
            if (!choices.isEmpty()) {
                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard") +" ";
                Map<String, Object> params = Maps.newHashMap();
                params.put("Attackers", cards);
                Card choosen = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, false, params);

                if (choosen != null) {
                    tgtCards.add(choosen);
                }
            }
        } else {
            tgtCards.addAll(getTargetCards(sa));
        }

        final boolean mustBlockAll = sa.hasParam("BlockAllDefined");

        long ts = game.getNextTimestamp();

        for (final Card c : tgtCards) {
            if (mustBlockAll) {
                c.addMustBlockCards(ts, cards);
            } else {
                final Card attacker = cards.get(0);
                c.addMustBlockCard(ts, attacker);
            }
        }

        if (sa.hasParam("Duration")) {
            final GameCommand removeBlockingRequirements = new GameCommand() {
                private static final long serialVersionUID = -5861529814760561373L;

                @Override
                public void run() {
                    for (final Card c : tgtCards) {
                        c.removeMustBlockCards(ts);
                    }
                }
            };
            addUntilCommand(sa, removeBlockingRequirements);
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();

        // end standard pre-

        String attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedAttacker"), sa);
            attacker = cards.get(0).toString();
        } else {
            attacker = host.toString();
        }

        if (sa.hasParam("Choices")) {
            sb.append("Choosen creature ").append(" must block ").append(attacker).append(" if able.");
        } else {
            for (final Card c : getTargetCards(sa)) {
                sb.append(c).append(" must block ").append(attacker).append(" if able.");
            }
        }
        return sb.toString();
    }

}
