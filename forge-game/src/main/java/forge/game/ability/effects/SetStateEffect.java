package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

import java.util.Map;

public class SetStateEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();
        boolean specialize = sa.getParam("Mode").equals("Specialize");

        if (sa.hasParam("Flip")) {
            sb.append("Flip ");
        } else if (specialize) { // verb will come later
        } else {
            sb.append("Transform ");
        }

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        if (specialize) {
            sb.append(" perpetually specializes into ");
            sb.append(host.hasChosenColor() ? host.getChosenColor() : "the chosen color");
        }
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

        CardCollectionView cardsToTransform;
        if (sa.hasParam("Choices")) {
            CardCollectionView choices = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("Choices"), p, host, sa);

            final int validAmount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "1"), sa);
            final int minAmount = sa.hasParam("MinAmount") ? Integer.parseInt(sa.getParam("MinAmount")) : validAmount;

            if (validAmount <= 0) {
                return;
            }

            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") :
                    Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            cardsToTransform = p.getController().chooseCardsForEffect(choices, sa, title, minAmount, validAmount,
                    !sa.hasParam("Mandatory"), null);
        } else {
            cardsToTransform = getTargetCards(sa);
        }

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Card tgtCard : cardsToTransform) {
            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(tgtCard, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtCard.equalsWithGameTimestamp(gameCard)) {
                continue;
            }

            // Cards which are not on the battlefield should not be able to transform.
            // TurnFace should be allowed in other zones like Exile too
            // Specialize and Unspecialize are allowed in other zones
            if (!"TurnFace".equals(mode) && !"Unspecialize".equals(mode) && !"Specialize".equals(mode)
                    && !gameCard.isInPlay() && !sa.hasParam("ETB")) {
                continue;
            }

            // facedown cards that are not Permanent, can't turn faceup there
            if ("TurnFace".equals(mode) && gameCard.isFaceDown() && gameCard.isInPlay()) {
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
            if ("TurnFace".equals(mode) && !gameCard.isFaceDown() && gameCard.isInPlay()
                    && gameCard.hasMergedCard()) {
                boolean hasBackSide = false;
                for (final Card c : gameCard.getMergedCards()) {
                    if (c.isDoubleFaced()) {
                        hasBackSide = true;
                        break;
                    }
                }
                if (hasBackSide) {
                    continue;
                }
            }

            // for reasons it can't transform, skip
            if ("Transform".equals(mode) && !gameCard.canTransform(sa)) {
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
            } else if ("Specialize".equals(mode)) {
                hasTransformed = gameCard.changeCardState(mode, host.getChosenColor(), sa);
                host.setChosenColors(null);
            } else {
                hasTransformed = gameCard.changeCardState(mode, sa.getParam("NewState"), sa);
                if (gameCard.isFaceDown() && (sa.hasParam("FaceDownPower") || sa.hasParam("FaceDownToughness")
                        || sa.hasParam("FaceDownSetType"))) {
                    CardFactoryUtil.setFaceDownState(gameCard, sa);
                }
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
                        String sb = p + " has revealed " + gameCard.getName() + " with the chosen names: " +
                                gameCard.getNamedCards();
                        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    } else {
                        String sb = p + " has revealed " + gameCard.getName() + " with the chosen name " + gameCard.getNamedCard();
                        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    }
                }
                game.fireEvent(new GameEventCardStatsChanged(gameCard));
                if (sa.hasParam("Mega")) { // TODO move Megamorph into an Replacement Effect
                    gameCard.addCounter(CounterEnumType.P1P1, 1, p, table);
                }
                if (remChanged) {
                    host.addRemembered(gameCard);
                }
                if (!gameCard.isTransformable())
                    transformedCards.add(gameCard);
                if ("Specialize".equals(mode)) {
                    gameCard.setSpecialized(true);
                    //run Specializes trigger
                    final TriggerHandler th = game.getTriggerHandler();
                    th.clearActiveTriggers(gameCard, null);
                    th.registerActiveTrigger(gameCard, false);
                    final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(gameCard);
                    th.runTrigger(TriggerType.Specializes, runParams, false);
                } else if ("Unspecialize".equals(mode)) {
                    gameCard.setSpecialized(false);
                }
            }
        }
        table.replaceCounterEffect(game, sa, true);
        if (!transformedCards.isEmpty()) {
            game.getAction().reveal(transformedCards, p, true, "Transformed cards in ");
        }
    }
}
