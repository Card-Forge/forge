package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

public class SetStateEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa.hasParam("Flip")) {
            sb.append("Flip ");
        } else {
            sb.append("Transform ");
        }

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final String mode = sa.getParam("Mode");
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        final boolean remChanged = sa.hasParam("RememberChanged");
        final boolean hiddenAgenda = sa.hasParam("HiddenAgenda");
        final boolean optional = sa.hasParam("Optional");
        final CardCollection transformedCards = new CardCollection();

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Card tgtCard : getTargetCards(sa)) {
            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(tgtCard, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtCard.equalsWithTimestamp(gameCard)) {
                continue;
            }

            if (sa.usesTargeting() && !gameCard.canBeTargetedBy(sa)) {
                continue;
            }

            // Cards which are not on the battlefield should not be able to transform.
            // TurnFace should be allowed in other zones like Exil too
            if (!"TurnFace".equals(mode) && !gameCard.isInZone(ZoneType.Battlefield)) {
                continue;
            }

            // facedown cards that are not Permanent, can't turn faceup there
            if ("TurnFace".equals(mode) && gameCard.isFaceDown() && gameCard.isInZone(ZoneType.Battlefield)) {
                if (gameCard.hasMergedCard()) {
                    boolean hasNonPermanent = false;
                    Card nonPermanentCard = null;
                    for (final Card c : gameCard.getMergedCards()) {
                        if (!c.getState(CardStateName.Original).getType().isPermanent()) {
                            hasNonPermanent = true;
                            nonPermanentCard = c;
                            break;
                        }
                    }
                    if (hasNonPermanent) {
                        Card lki = CardUtil.getLKICopy(nonPermanentCard);
                        lki.forceTurnFaceUp();
                        game.getAction().reveal(new CardCollection(lki), lki.getOwner(), true, Localizer.getInstance().getMessage("lblFaceDownCardCantTurnFaceUp"));
                        continue;
                    }
                } else if (!gameCard.getState(CardStateName.Original).getType().isPermanent()) {
                    Card lki = CardUtil.getLKICopy(gameCard);
                    lki.forceTurnFaceUp();
                    game.getAction().reveal(new CardCollection(lki), lki.getOwner(), true, Localizer.getInstance().getMessage("lblFaceDownCardCantTurnFaceUp"));

                    continue;
                }
            }

            // Merged faceup permanent that have double faced cards can't turn face down
            if ("TurnFace".equals(mode) && !gameCard.isFaceDown() && gameCard.isInZone(ZoneType.Battlefield)
                    && gameCard.hasMergedCard()) {
                boolean hasBackSide = false;
                for (final Card c : gameCard.getMergedCards()) {
                    if (c.hasBackSide()) {
                        hasBackSide = true;
                        break;
                    }
                }
                if (hasBackSide) {
                    continue;
                }
            }

            // for reasons it can't transform, skip
            if ("Transform".equals(mode) && !gameCard.canTransform()) {
                continue;
            }

            if ("Transform".equals(mode) && gameCard.equals(host) && sa.hasSVar("StoredTransform")) {
                // If want to Transform, and host is trying to transform self, skip if not in alignment
                boolean skip = gameCard.getTransformedTimestamp() != Long.parseLong(sa.getSVar("StoredTransform"));
                // Clear SVar from SA so it doesn't get reused accidentally
                sa.removeSVar("StoredTransform");
                if (skip) {
                    continue;
                }
            }

            if (optional) {
                String message = TextUtil.concatWithSpace("Transform", gameCard.getName(), "?");
                if (!p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, message, null)) {
                    return;
                }
            }

            boolean hasTransformed = false;
            if (sa.isMorphUp()) {
                hasTransformed = gameCard.turnFaceUp(sa);
            } else if (sa.isManifestUp()) {
                hasTransformed = gameCard.turnFaceUp(true, true, sa);
            } else {
                hasTransformed = gameCard.changeCardState(mode, sa.getParam("NewState"), sa);
            }
            if (hasTransformed) {
                if (sa.isMorphUp()) {
                    String sb = p + " has unmorphed " + gameCard.getName();
                    game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                } else if (sa.isManifestUp()) {
                    String sb = p + " has unmanifested " + gameCard.getName();
                    game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                } else if (hiddenAgenda) {
                    if (gameCard.hasKeyword("Double agenda")) {
                        String sb = p + " has revealed " + gameCard.getName() + " with the chosen names " +
                                gameCard.getNamedCard() + " and " + gameCard.getNamedCard2();
                        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    } else {
                        String sb = p + " has revealed " + gameCard.getName() + " with the chosen name " + gameCard.getNamedCard();
                        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    }
                }
                game.fireEvent(new GameEventCardStatsChanged(gameCard));
                if (sa.hasParam("Mega")) {
                    gameCard.addCounter(CounterEnumType.P1P1, 1, p, true, table);
                }
                if (remChanged) {
                    host.addRemembered(gameCard);
                }
                transformedCards.add(gameCard);
            }
        }
        table.triggerCountersPutAll(game);
        if (!transformedCards.isEmpty()) {
            game.getAction().reveal(transformedCards, p, true, "Transformed cards in ");
        }
    }
}
