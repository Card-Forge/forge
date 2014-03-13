package forge.screens.match.input;

import forge.FThreads;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.player.HumanPlay;
import forge.screens.match.FControl;
import forge.toolbox.VCardZoom.ZoomController;
import forge.utils.Evaluator;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class InputPayMana extends InputSyncronizedBase {
    private static final long serialVersionUID = -9133423708688480255L;

    protected int phyLifeToLose = 0;

    protected final Player player;
    protected final Game game;
    protected ManaCostBeingPaid manaCost;
    protected final SpellAbility saPaidFor;

    private boolean bPaid = false;
    private Boolean canPayManaCost = null;

    private boolean locked = false;

    protected InputPayMana(SpellAbility saToPayFor, Player payer) {
        this.player = payer;
        this.game = player.getGame();
        this.saPaidFor = saToPayFor;
    }

    @Override
    protected final void onCardSelected(final Card card, final List<Card> orderedCardOptions) {
        if (locked) {
            System.err.print("Should wait till previous call to playAbility finishes.");
            return;
        }

        FControl.getView().getCardZoom().show(FControl.getView().getPrompt().getMessage(),
                card, orderedCardOptions, new ZoomController<SpellAbility>() {
            private byte colorCanUse, colorNeeded;

            @Override
            public List<SpellAbility> getOptions(final Card card) {
                if (card.getController() != player || card.getManaAbility().isEmpty()) {
                    return null;
                }

                colorCanUse = 0;
                colorNeeded = 0;

                for (final byte color : MagicColor.WUBRG) {
                    if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) { colorCanUse |= color; }
                    if (manaCost.needsColor(color, player.getManaPool()))           { colorNeeded |= color; }
                }
                if (manaCost.isAnyPartPayableWith((byte) ManaAtom.COLORLESS, player.getManaPool())) 
                    colorCanUse |= ManaAtom.COLORLESS;

                if (colorCanUse == 0) { // no mana cost or something 
                    return null;
                }

                // you can't remove unneeded abilities inside a for (am:abilities) loop :(
                final String typeRes = manaCost.getSourceRestriction();
                if (StringUtils.isNotBlank(typeRes) && !card.isType(typeRes)) {
                    return null;
                }

                boolean guessAbilityWithRequiredColors = true;
                List<SpellAbility> abilities = new ArrayList<SpellAbility>();

                for (SpellAbility ma : card.getManaAbility()) {
                    ma.setActivatingPlayer(player);

                    AbilityManaPart m = ma.getManaPartRecursive();
                    if (m == null || !ma.canPlay())                                 { continue; }
                    if (!abilityProducesManaColor(ma, m, colorCanUse))              { continue; }
                    if (ma.isAbility() && ma.getRestrictions().isInstantSpeed())    { continue; }
                    if (!m.meetsManaRestrictions(saPaidFor))                        { continue; }

                    abilities.add(ma);

                    // skip express mana if the ability is not undoable or reusable
                    if (!ma.isUndoable() || !ma.getPayCosts().isRenewableResource() || ma.getSubAbility() != null) {
                        guessAbilityWithRequiredColors = false;
                    }
                }

                if (abilities.isEmpty()) {
                    return abilities;
                }

                // Store some information about color costs to help with any mana choices
                if (colorNeeded == 0) {  // only colorless left
                    if (saPaidFor.getHostCard() != null && saPaidFor.getHostCard().hasSVar("ManaNeededToAvoidNegativeEffect")) {
                        String[] negEffects = saPaidFor.getHostCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
                        for (String negColor : negEffects) {
                            byte col = MagicColor.fromName(negColor);
                            colorCanUse |= col;
                        }
                    }
                }

                // If the card has any ability that tracks mana spent, skip express Mana choice
                if (saPaidFor.tracksManaSpent()) {
                    colorCanUse = MagicColor.ALL_COLORS;
                    guessAbilityWithRequiredColors = false;
                }

                if (guessAbilityWithRequiredColors) {
                    // express Mana Choice
                    if (colorNeeded == 0) {
                        //avoid unnecessary prompt by pretending we need White
                        //for the sake of "Add one mana of any color" effects
                        colorNeeded = MagicColor.WHITE;
                    }
                    else {
                        final ArrayList<SpellAbility> colorMatches = new ArrayList<SpellAbility>();
                        for (SpellAbility sa : abilities) {
                            if (abilityProducesManaColor(sa, sa.getManaPartRecursive(), colorNeeded)) {
                                colorMatches.add(sa);
                            }
                        }

                        if (!colorMatches.isEmpty() && colorMatches.size() < abilities.size()) {
                            // leave behind only color matches
                            abilities = colorMatches;
                        }
                    }
                }

                return abilities;
            }

            @Override
            public boolean selectOption(final Card card, final SpellAbility option) {
                ColorSet colors = ColorSet.fromMask(0 == colorNeeded ? colorCanUse : colorNeeded);
                option.getManaPartRecursive().setExpressChoice(colors);

                Runnable proc = new Runnable() {
                    @Override
                    public void run() {
                        HumanPlay.playSpellAbility(option.getActivatingPlayer(), option);
                        player.getManaPool().payManaFromAbility(saPaidFor, manaCost, option);

                        onManaAbilityPaid();
                        onStateChanged();
                    }
                };
                locked = true;
                game.getAction().invoke(proc);
                return false;
            }
        });
    }

    public void useManaFromPool(byte colorCode) {
        // find the matching mana in pool.
        player.getManaPool().tryPayCostWithColor(colorCode, saPaidFor, manaCost);
        onManaAbilityPaid();
        showMessage();
    }

    /**
     * <p>
     * canMake.  color is like "G", returns "Green".
     * </p>
     * 
     * @param am
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static boolean abilityProducesManaColor(final SpellAbility am, AbilityManaPart m, final byte neededColor) {
        if (0 != (neededColor & MagicColor.COLORLESS)) {
            return true;
        }

        if (m.isAnyMana()) {
            return true;
        }

        // check for produce mana replacement effects - they mess this up, so just use the mana ability
        final Card source = am.getHostCard();
        final Player activator = am.getActivatingPlayer();
        final Game g = source.getGame();
        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "ProduceMana");
        repParams.put("Mana", m.getOrigProduced());
        repParams.put("Affected", source);
        repParams.put("Player", activator);
        repParams.put("AbilityMana", am);

        for (final Player p : g.getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (replacementEffect.requirementsCheck(g)
                            && replacementEffect.canReplace(repParams)
                            && replacementEffect.getMapParams().containsKey("ManaReplacement")
                            && replacementEffect.zonesCheck(g.getZoneOf(crd))) {
                        return true;
                    }
                }
            }
        }

        if (am.getApi() == ApiType.ManaReflected) {
            final Iterable<String> reflectableColors = CardUtil.getReflectableManaColors(am);
            for (final String color : reflectableColors) {
                if (0 != (neededColor & MagicColor.fromName(color))) {
                    return true;
                }
            }
        }
        else {
            String colorsProduced = m.isComboMana() ? m.getComboColors() : m.getOrigProduced();
            for (final String color : colorsProduced.split(" ")) {
                if (0 != (neededColor & MagicColor.fromName(color))) {
                    return true;
                }
                if( (neededColor & ManaAtom.COLORLESS) != 0)
                    return true;
            }
        }
        return false;
    }

    protected boolean isAlreadyPaid() {
        if (manaCost.isPaid()) {
            bPaid = true;
        }
        return bPaid;
    }

    protected boolean supportAutoPay() {
        return true;
    }

    private void runAsAi(Runnable proc) {
        this.player.runWithController(proc, new PlayerControllerAi(this.game, this.player, this.player.getOriginalLobbyPlayer()));
    }

    /** {@inheritDoc} */
    @Override
    protected void onOk() {
        if (supportAutoPay()) {
            //use AI utility to automatically pay mana cost if possible
            final Runnable proc = new Runnable() {
                @Override
                public void run() {
                    ComputerUtilMana.payManaCost(manaCost, saPaidFor, player);
                }
            };
            //must run in game thread as certain payment actions can only be automated there
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    runAsAi(proc);
                    onStateChanged();
                }
            });
        }
    }

    protected void updateButtons() {
        if (supportAutoPay()) {
            ButtonUtil.setButtonText("Auto", "Cancel");
        }
        ButtonUtil.enableOnlyCancel();
    }

    protected final void updateMessage() {
        locked = false;
        if (supportAutoPay()) {
            if (canPayManaCost == null) {
                //use AI utility to determine if mana cost can be paid if that hasn't been determined yet
                Evaluator<Boolean> proc = new Evaluator<Boolean>() {
                    @Override
                    public Boolean evaluate() {
                        return ComputerUtilMana.canPayManaCost(manaCost, saPaidFor, player);
                    }
                };
                runAsAi(proc);
                canPayManaCost = proc.getResult();
            }
            if (canPayManaCost) {
                ButtonUtil.enableAll(); //enabled Auto button if mana cost can be paid
            }
        }
        showMessage(getMessage());
    }

    /** {@inheritDoc} */
    @Override
    protected final void onStop() {
        if (supportAutoPay()) {
            ButtonUtil.reset();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        if (isFinished()) { return; }
        updateButtons();
        onStateChanged();
    }

    protected void onStateChanged() {
        if (isAlreadyPaid()) {
            done();
            stop();
        }
        else {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() {
                    updateMessage();
                }
            });
        }
    }

    protected void onManaAbilityPaid() {} // some inputs overload it
    protected abstract void done();
    protected abstract String getMessage();

    @Override
    public String toString() {
        return String.format("PayManaBase %s left", manaCost.toString());
    }

    public boolean isPaid() { return bPaid; }

    protected String messagePrefix;
    public void setMessagePrefix(String prompt) {
        // TODO Auto-generated method stub
        messagePrefix = prompt;
    }
}
