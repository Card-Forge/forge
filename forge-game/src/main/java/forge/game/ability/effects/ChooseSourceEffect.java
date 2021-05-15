package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class ChooseSourceEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a source.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        CardCollection stackSources = new CardCollection();
        CardCollection referencedSources = new CardCollection();
        CardCollection commandZoneSources = new CardCollection();
        CardCollection sourcesToChooseFrom = new CardCollection();

        // Get the list of permanent cards
        CardCollectionView permanentSources = game.getCardsIn(ZoneType.Battlefield);

        // A source can be a face-up card in the command zone
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (!c.isFaceDown()) {
                commandZoneSources.add(c);
            }
        }

        // Get the list of cards that produce effects on the stack
        for (SpellAbilityStackInstance stackinst : game.getStack()) {
            stackSources.add(stackinst.getSourceCard());

            // Get the list of cards that are referenced by effects on the stack
            SpellAbility siSpellAbility = stackinst.getSpellAbility(true);
            if (siSpellAbility.getTriggeringObjects() != null) {
                for (Object c : siSpellAbility.getTriggeringObjects().values()) {
                    if (c instanceof Card) {
                        if (!stackSources.contains(c)) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
            }
            if (siSpellAbility.getTargetCard() != null) {
                referencedSources.add(siSpellAbility.getTargetCard());
            }
            // TODO: is this necessary?
            if (siSpellAbility.getReplacingObjects() != null) {
                for (Object c : siSpellAbility.getReplacingObjects().values()) {
                    if (c instanceof Card) {
                        if (!stackSources.contains(c)) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
            }
        }

        if (sa.hasParam("Choices")) {
            permanentSources = CardLists.getValidCards(permanentSources, sa.getParam("Choices"), host.getController(), host, sa);

            stackSources = CardLists.getValidCards(stackSources, sa.getParam("Choices"), host.getController(), host, sa);
            referencedSources = CardLists.getValidCards(referencedSources, sa.getParam("Choices"), host.getController(), host, sa);
            commandZoneSources = CardLists.getValidCards(commandZoneSources, sa.getParam("Choices"), host.getController(), host, sa);
        }
        if (sa.hasParam("TargetControls")) {
            permanentSources = CardLists.filterControlledBy(permanentSources, tgtPlayers.get(0));
            stackSources = CardLists.filterControlledBy(stackSources, tgtPlayers.get(0));
            referencedSources = CardLists.filterControlledBy(referencedSources, tgtPlayers.get(0));
            commandZoneSources = CardLists.filterControlledBy(commandZoneSources, tgtPlayers.get(0));
        }

        Card divPermanentSources = new Card(-1, game);
        divPermanentSources.setName("--PERMANENTS:--");
        Card divStackSources = new Card(-2, game);
        divStackSources.setName("--SPELLS ON THE STACK:--");
        Card divReferencedSources = new Card(-3, game);
        divReferencedSources.setName("--OBJECTS REFERRED TO ON THE STACK:--");
        Card divCommandZoneSources = new Card(-4, game);
        divCommandZoneSources.setName("--CARDS IN THE COMMAND ZONE:--");

        if (!permanentSources.isEmpty()) {
            sourcesToChooseFrom.add(divPermanentSources);
            sourcesToChooseFrom.addAll(permanentSources);
        }
        if (!stackSources.isEmpty()) {
            sourcesToChooseFrom.add(divStackSources);
            sourcesToChooseFrom.addAll(stackSources);
        }
        if (!referencedSources.isEmpty()) {
            sourcesToChooseFrom.add(divReferencedSources);
            sourcesToChooseFrom.addAll(referencedSources);
        }
        if (!commandZoneSources.isEmpty()) {
            sourcesToChooseFrom.add(divCommandZoneSources);
            sourcesToChooseFrom.addAll(commandZoneSources);
        }

        if (sourcesToChooseFrom.isEmpty()) {
            return;
        }

        final String numericAmount = sa.getParamOrDefault("Amount", "1");
        final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) : AbilityUtils.calculateAmount(host, numericAmount, sa);

        for (final Player p : tgtPlayers) {
            final CardCollection chosen = new CardCollection();
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    final String choiceTitle = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseSource") + " ";
                    Card o = null;
                    do {
                        o = p.getController().chooseSingleEntityForEffect(sourcesToChooseFrom, sa, choiceTitle, null);
                    } while (o == null);
                    chosen.add(o);
                    sourcesToChooseFrom.remove(o);
                }
                host.setChosenCards(chosen);
                if (sa.hasParam("RememberChosen")) {
                    for (final Card rem : chosen) {
                        host.addRemembered(rem);
                    }
                }
            }
        }
    }
}
