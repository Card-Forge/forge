package forge.game.decision;

/** Explicit semantic profiles admitted by confirmation production slices. */
public enum ConfirmationTriggerProfile {
    GELECTRODE_SPELL_CAST_UNTAP_SELF("GELECTRODE_CONFIRMATION"),
    BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD("BLOOD_ETB_CONFIRMATION");

    private final String traceLabel;

    ConfirmationTriggerProfile(final String traceLabel0) {
        traceLabel = traceLabel0;
    }

    public String getTraceLabel() {
        return traceLabel;
    }
}
