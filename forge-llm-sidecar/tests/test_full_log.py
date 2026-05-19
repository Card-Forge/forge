"""Integration test: parse the full Atlin vs Buthomar game log."""

from __future__ import annotations

from pathlib import Path

from app.forge_log.parser import parse_line, parse_lines
from app.forge_log import ForgeLogAdapter

LOG_PATH = Path(__file__).parent / "test_log.txt"

LOG_CONTENT = LOG_PATH.read_text()


class TestFullLogParse:
    """Parse every line of the real game log and verify no lines are lost."""

    def setup_method(self):
        self.lines = [l for l in LOG_CONTENT.splitlines() if l.strip()]
        self.events = parse_lines(self.lines)
        self.unparsed = [
            (i + 1, line)
            for i, line in enumerate(self.lines)
            if parse_line(line, i + 1) is None
        ]

    def test_no_unparsed_lines(self):
        """Every non-empty line should parse to a recognised event."""
        assert not self.unparsed, (
            f"{len(self.unparsed)} lines failed to parse:\n"
            + "\n".join(f"  L{n}: {raw!r}" for n, raw in self.unparsed[:20])
        )

    def test_event_count(self):
        """Sanity check: expect ~500 events from a 25-turn game."""
        assert len(self.events) > 400, f"Only got {len(self.events)} events"

    def test_all_turns_present(self):
        """Turns 1-25 should all be present."""
        from app.forge_log.events import TurnBegan
        turns = {
            ev.turn_number
            for ev in self.events
            if isinstance(ev, TurnBegan)
        }
        expected = set(range(1, 26))
        missing = expected - turns
        assert not missing, f"Missing turns: {missing}"

    def test_players_detected(self):
        """Both Atlin and Buthomar should appear as players."""
        from app.forge_log.events import TurnBegan
        players = {
            ev.player
            for ev in self.events
            if isinstance(ev, TurnBegan)
        }
        assert "Atlin" in players
        assert "Buthomar" in players

    def test_final_life_totals(self):
        """Atlin lost (life reached 0).  Buthomar won."""
        from app.forge_log.events import GameOutcome
        outcomes = [
            ev for ev in self.events if isinstance(ev, GameOutcome)
        ]
        atlin_lost = any(
            ev.player == "Atlin" and ev.lost for ev in outcomes
        )
        buthomar_won = any(
            ev.player == "Buthomar" and ev.won for ev in outcomes
        )
        assert atlin_lost, "Atlin should have a loss outcome"
        assert buthomar_won, "Buthomar should have a win outcome"

    def test_spell_casts(self):
        """Expect multiple spell casts from both players."""
        from app.forge_log.events import SpellCast
        spells = [ev for ev in self.events if isinstance(ev, SpellCast)]
        atlin_spells = {ev.card for ev in spells if ev.player == "Atlin"}
        buthomar_spells = {ev.card for ev in spells if ev.player == "Buthomar"}
        assert len(atlin_spells) > 5, f"Atlin only cast {len(atlin_spells)} unique spells"
        assert len(buthomar_spells) > 5, f"Buthomar only cast {len(buthomar_spells)} unique spells"

    def test_damage_events(self):
        """Expect multiple damage events."""
        from app.forge_log.events import DamageEvent
        damages = [ev for ev in self.events if isinstance(ev, DamageEvent)]
        assert len(damages) > 10

    def test_zone_changes(self):
        """Expect zone change events."""
        from app.forge_log.events import ZoneChange
        zones = [ev for ev in self.events if isinstance(ev, ZoneChange)]
        assert len(zones) > 10

    def test_land_played(self):
        """Expect land played events."""
        from app.forge_log.events import LandPlayed
        lands = [ev for ev in self.events if isinstance(ev, LandPlayed)]
        assert len(lands) > 10

    def test_discard_events(self):
        """Expect discard events."""
        from app.forge_log.events import Discard
        discards = [ev for ev in self.events if isinstance(ev, Discard)]
        assert len(discards) > 5

    def test_mana_events(self):
        """Expect mana events."""
        from app.forge_log.events import ManaEvent
        manas = [ev for ev in self.events if isinstance(ev, ManaEvent)]
        assert len(manas) > 20


class TestAdapterFullLog:
    """End-to-end adapter test on the full log."""

    def test_adapter_parse(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        assert len(cps) > 20, f"Only {len(cps)} checkpoints"

    def test_adapter_turns(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        turns = {cp.turn for cp in cps}
        assert len(turns) >= 20, f"Only {len(turns)} unique turns in checkpoints"

    def test_adapter_final_life(self):
        """Final checkpoint should reflect game-ending life total."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        final = cps[-1]
        # Life reaching 0 or below triggers a checkpoint
        assert final.life_totals.get("Atlin") == -2
        assert final.life_totals.get("Buthomar") == 12

    def test_adapter_state_final_life_after_all_events(self):
        """State should reflect final life after processing all events."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        adapter.parse(LOG_CONTENT)
        # After all events processed, Atlin should be at -2
        assert adapter.state.players["Atlin"].life == -2
        assert adapter.state.players["Buthomar"].life == 12

    def test_adapter_opponent_board(self):
        """Opponent board should contain lands and permanents."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        final = cps[-1]
        # Atlin played multiple lands
        assert len(final.opponent_board) > 3

    def test_adapter_own_board(self):
        """AI board should track Buthomar's permanents."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        final = cps[-1]
        # Buthomar played multiple lands
        assert len(final.own_board) > 3

    def test_adapter_graveyard(self):
        """Graveyards should be tracked."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        final = cps[-1]
        # Both players should have graveyard contents
        assert len(final.opponent_graveyard) > 3
        assert len(final.your_graveyard) > 0

    def test_adapter_observations(self):
        """Observations should track opponent plays."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        final = cps[-1]
        # Should have observations of Atlin's spells and lands
        assert len(final.observations) > 5

    def test_adapter_opponent_land_count(self):
        """Adapter should count opponent land plays correctly."""
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Buthomar")
        cps = adapter.parse(LOG_CONTENT)
        land_obs = [o for o in cps[-1].observations if o.event == "land"]
        spell_obs = [o for o in cps[-1].observations if o.event == "spell"]
        assert len(land_obs) > 3, f"Only {len(land_obs)} land observations"
        assert len(spell_obs) > 3, f"Only {len(spell_obs)} spell observations"
