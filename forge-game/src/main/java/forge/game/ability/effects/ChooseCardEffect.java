package forge.game.ability.effects;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ChooseCardEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a card.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final CardCollection chosen = new CardCollection();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        ZoneType choiceZone = ZoneType.Battlefield;
        if (sa.hasParam("ChoiceZone")) {
            choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
        }
        CardCollectionView choices = game.getCardsIn(choiceZone);
        if (sa.hasParam("Choices")) {
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host);
        }
        if (sa.hasParam("TargetControls")) {
            choices = CardLists.filterControlledBy(choices, tgtPlayers.get(0));
        }
        if (sa.hasParam("DefinedCards")) {
            choices = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedCards"), sa);
        }

        final String numericAmount = sa.getParamOrDefault("Amount", "1");
        final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) : CardFactoryUtil.xCount(host, host.getSVar(numericAmount));
        final int minAmount = sa.hasParam("MinAmount") ? Integer.parseInt(sa.getParam("MinAmount")) : validAmount;

        if (validAmount <= 0) {
            return;
        }

        for (final Player p : tgtPlayers) {
            if (sa.hasParam("EachBasicType")) {
                // Get all lands, 
                List<Card> land = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
                String eachBasic = sa.getParam("EachBasicType");
                if (eachBasic.equals("Controlled")) {
                    land = CardLists.filterControlledBy(land, p);
                }
                
                // Choose one of each BasicLand given special place
                for (final String type : CardType.getBasicTypes()) {
                    final CardCollectionView cl = CardLists.getType(land, type);
                    if (!cl.isEmpty()) {
                        final String prompt = "Choose " + Lang.nounWithAmount(1, type);
                        Card c = p.getController().chooseSingleEntityForEffect(cl, sa, prompt, false);
                        if (c != null) {
                            chosen.add(c);
                        }
                    }
                }
            } else if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.hasParam("AtRandom") && !choices.isEmpty()) {
                    Aggregates.random(choices, validAmount, chosen);
                } else {
                    String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a card ";
                    chosen.addAll(p.getController().chooseCardsForEffect(choices, sa, title, minAmount, validAmount, !sa.hasParam("Mandatory")));
                }
            }
        }
        host.setChosenCards(chosen);
        if (sa.hasParam("RememberChosen")) {
            for (final Card rem : chosen) {
                host.addRemembered(rem);
            }
        }
        if (sa.hasParam("ForgetChosen")) {
            for (final Card rem : chosen) {
                host.removeRemembered(rem);
            }
        }
    }
}
