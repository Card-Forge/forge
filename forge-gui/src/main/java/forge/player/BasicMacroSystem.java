package forge.player;

import com.google.common.collect.Lists;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.PlayerAction;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputPassPriority;
import forge.interfaces.IMacroSystem;
import forge.localinstance.skin.FSkinProp;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

// Simple macro system implementation. Its goal is to simulate "clicking"
// on cards/players in an automated way, to reduce mechanical overhead of
// situations like repeated combo activation.
public class BasicMacroSystem implements IMacroSystem {
    private final PlayerControllerHuman playerControllerHuman;
    // Position in the macro "sequence".
    private int sequenceIndex = 0;
    // "Actions" are stored as a pair of the "action" recipient (the entity
    // to "click") and a boolean representing whether the entity is a player.
    private final List<Pair<GameEntityView, Boolean>> rememberedActions = Lists.newArrayList();
    private String rememberedSequenceText = "";

    private final Localizer localizer = Localizer.getInstance();

    public BasicMacroSystem(PlayerControllerHuman playerControllerHuman) {
        this.playerControllerHuman = playerControllerHuman;
    }

    @Override
    public void addRememberedAction(PlayerAction action) {
        // No-op this isn't really used for the old macro system
    }

    @Override
    public void setRememberedActions() {
        final String dialogTitle = localizer.getMessage("lblRememberActionSequence");
        // Not sure if this priority guard is really needed, but it seems
        // like an alright idea.
        final Input input = playerControllerHuman.inputQueue.getInput();
        if (!(input instanceof InputPassPriority)) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblYouMustHavePrioritytoUseThisFeature"), dialogTitle);
            return;
        }

        int currentIndex = sequenceIndex;
        sequenceIndex = 0;
        // Use a Pair so we can keep a flag for isPlayer
        final List<Pair<Integer, Boolean>> entityInfo = Lists.newArrayList();
        final int playerID = playerControllerHuman.getPlayer().getId();
        // Only support 1 opponent for now. There are some ideas about
        // supporting multiplayer games in the future, but for now it would complicate
        // the parsing process, and this implementation is still a "proof of concept".
        int opponentID = 0;
        for (final Player player : playerControllerHuman.getGame().getPlayers()) {
            if (player.getId() != playerID) {
                opponentID = player.getId();
                break;
            }
        }

        // A more informative prompt would be useful, but the dialog seems
        // to like to clip text in long messages...
        final String prompt = localizer.getMessage("lblEnterASequence");
        String textSequence = playerControllerHuman.getGui().showInputDialog(prompt, dialogTitle, FSkinProp.ICO_QUEST_NOTES,
                rememberedSequenceText);
        if (textSequence == null || textSequence.trim().isEmpty()) {
            rememberedActions.clear();
            if (!rememberedSequenceText.isEmpty()) {
                rememberedSequenceText = "";
                playerControllerHuman.getGui().message(localizer.getMessage("lblActionSequenceCleared"), dialogTitle);
            }
            return;
        }
        // If they haven't changed the sequence, inform them the index is
        // reset but don't change rememberedActions.
        if (textSequence.equals(rememberedSequenceText)) {
            if (currentIndex > 0 && currentIndex < rememberedActions.size()) {
                playerControllerHuman.getGui().message(localizer.getMessage("lblRestartingActionSequence"), dialogTitle);
            }
            return;
        }
        rememberedSequenceText = textSequence;
        rememberedActions.clear();

        // Clean up input
        textSequence = textSequence.trim().toLowerCase().replaceAll("[@%]", "");
        // Replace "opponent" and "me" with symbols to ease following replacements
        textSequence = textSequence.replaceAll("\\bopponent\\b", "%").replaceAll("\\bme\\b", "@");
        // Strip user input of anything that's not a
        // digit/comma/whitespace/special symbol
        textSequence = textSequence.replaceAll("[^\\d\\s,@%]", "");
        // Now change various allowed delimiters to something neutral
        textSequence = textSequence.replaceAll("(,\\s+|,|\\s+)", "_");
        final String[] splitSequence = textSequence.split("_");
        for (final String textID : splitSequence) {
            if (StringUtils.isNumeric(textID)) {
                entityInfo.add(Pair.of(Integer.valueOf(textID), false));
            } else if (textID.equals("%")) {
                entityInfo.add(Pair.of(opponentID, true));
            } else if (textID.equals("@")) {
                entityInfo.add(Pair.of(playerID, true));
            }
        }
        if (entityInfo.isEmpty()) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblErrorPleaseCheckID"), dialogTitle);
            return;
        }

        // Fetch cards and players specified by the user input
        final ZoneType[] zones = {ZoneType.Battlefield, ZoneType.Hand, ZoneType.Graveyard, ZoneType.Exile,
                ZoneType.Command};
        final CardCollectionView cards = playerControllerHuman.getGame().getCardsIn(Arrays.asList(zones));
        for (final Pair<Integer, Boolean> entity : entityInfo) {
            boolean found = false;
            // Nested loops are no fun; however, seems there's no better way
            // to get stuff by ID
            boolean isPlayer = entity.getValue();
            if (isPlayer) {
                for (final Player player : playerControllerHuman.getGame().getPlayers()) {
                    if (player.getId() == entity.getKey()) {
                        found = true;
                        rememberedActions.add(Pair.of(player.getView(), true));
                        break;
                    }
                }
            } else {
                for (final Card card : cards) {
                    if (card.getId() == entity.getKey()) {
                        found = true;
                        rememberedActions.add(Pair.of(card.getView(), false));
                        break;
                    }
                }
            }
            if (!found) {
                playerControllerHuman.getGui().message(localizer.getMessage("lblErrorEntityWithId") + " " + entity.getKey() + " " + localizer.getMessage("lblNotFound") + ".", dialogTitle);
                rememberedActions.clear();
                return;
            }
        }
    }

    @Override
    public void nextRememberedAction() {
        final String dialogTitle = localizer.getMessage("lblDoNextActioninSequence");
        if (rememberedActions.isEmpty()) {
            playerControllerHuman.getGui().message(localizer.getMessage("lblPleaseDefineanActionSequenceFirst"), dialogTitle);
            return;
        }
        if (sequenceIndex >= rememberedActions.size()) {
            // Wrap around to repeat the sequence
            sequenceIndex = 0;
        }
        final Pair<GameEntityView, Boolean> action = rememberedActions.get(sequenceIndex);
        final boolean isPlayer = action.getValue();
        if (isPlayer) {
            playerControllerHuman.selectPlayer((PlayerView) action.getKey(), new DummyTriggerEvent());
        } else {
            playerControllerHuman.selectCard((CardView) action.getKey(), null, new DummyTriggerEvent());
        }
        sequenceIndex++;
    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public String playbackText() {
        if (!"".equals(rememberedSequenceText)) {
            return new StringBuilder().append(sequenceIndex).append(" / ").append(rememberedActions.size()).toString();
        }
        return null;
    }

    private class DummyTriggerEvent implements ITriggerEvent {
        @Override
        public int getButton() {
            return 1; // Emulate left mouse button
        }

        @Override
        public int getX() {
            return 0; // Hopefully this doesn't do anything wonky!
        }

        @Override
        public int getY() {
            return 0;
        }
    }
}
