package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);


        List<Card> permanentSources = new ArrayList<Card>();
        List<Card> stackSources = new ArrayList<Card>();
        List<Card> referencedSources = new ArrayList<Card>();
        List<Card> commandZoneSources = new ArrayList<Card>();

        List<Card> sourcesToChooseFrom = new ArrayList<Card>();

        // Get the list of permanent cards
        permanentSources = game.getCardsIn(ZoneType.Battlefield);
        // A source can be a face-up card in the command zone
        commandZoneSources = new ArrayList<Card>();
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (!c.isFaceDown()) {
                commandZoneSources.add(c);
            }
        }

        // Get the list of cards that produce effects on the stack

        for (SpellAbilityStackInstance stackinst : game.getStack()) {
            if (!stackSources.contains(stackinst.getSourceCard())) {
                stackSources.add(stackinst.getSourceCard());
            }
            // Get the list of cards that are referenced by effects on the stack
            SpellAbility siSpellAbility = stackinst.getSpellAbility(true);
            if (siSpellAbility.getTriggeringObjects() != null) {
                for (Object c : siSpellAbility.getTriggeringObjects().values()) {
                    if (c instanceof Card) {
                        if (!stackSources.contains((Card) c)) {
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
                        if (!stackSources.contains((Card) c)) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
            }
        }

        if (sa.hasParam("Choices")) {
            permanentSources = CardLists.getValidCards(permanentSources, sa.getParam("Choices"), host.getController(), host);

            stackSources = CardLists.getValidCards(stackSources, sa.getParam("Choices"), host.getController(), host);
            referencedSources = CardLists.getValidCards(referencedSources, sa.getParam("Choices"), host.getController(), host);
            commandZoneSources = CardLists.getValidCards(commandZoneSources, sa.getParam("Choices"), host.getController(), host);
        }
        if (sa.hasParam("TargetControls")) {
            permanentSources = CardLists.filterControlledBy(permanentSources, tgtPlayers.get(0));
            stackSources = CardLists.filterControlledBy(stackSources, tgtPlayers.get(0));
            referencedSources = CardLists.filterControlledBy(referencedSources, tgtPlayers.get(0));
            commandZoneSources = CardLists.filterControlledBy(commandZoneSources, tgtPlayers.get(0));
        }

        Card divPermanentSources = new Card(-1);
        divPermanentSources.setName("--PERMANENTS:--");
        Card divStackSources = new Card(-2);
        divStackSources.setName("--SPELLS ON THE STACK:--");
        Card divReferencedSources = new Card(-3);
        divReferencedSources.setName("--OBJECTS REFERRED TO ON THE STACK:--");
        Card divCommandZoneSources = new Card(-4);
        divCommandZoneSources.setName("--CARDS IN THE COMMAND ZONE:--");

        if (permanentSources.size() > 0) {
            sourcesToChooseFrom.add(divPermanentSources);
            sourcesToChooseFrom.addAll(permanentSources);
        }
        if (stackSources.size() > 0) {
            sourcesToChooseFrom.add(divStackSources);
            sourcesToChooseFrom.addAll(stackSources);
        }
        if (referencedSources.size() > 0) {
            sourcesToChooseFrom.add(divReferencedSources);
            sourcesToChooseFrom.addAll(referencedSources);
        }
        if (commandZoneSources.size() > 0) {
            sourcesToChooseFrom.add(divCommandZoneSources);
            sourcesToChooseFrom.addAll(commandZoneSources);
        }

        if (sourcesToChooseFrom.size() == 0) {
            return;
        }

        final String numericAmount = sa.getParamOrDefault("Amount", "1");
        final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) : CardFactoryUtil.xCount(host, host.getSVar(numericAmount));

        for (final Player p : tgtPlayers) {
            final CardCollection chosen = new CardCollection();
            if (tgt == null || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    final String choiceTitle = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a source ";
                    Card o = null;
                    do {
                        o = p.getController().chooseSingleEntityForEffect(sourcesToChooseFrom, sa, choiceTitle);
                    } while (o.equals(divPermanentSources) || o.equals(divStackSources) || o.equals(divReferencedSources) || o.equals(divCommandZoneSources));
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
