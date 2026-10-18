package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// The Count$...DamageThisTurn family is xCount's DamageThisTurn branch
// (AbilityUtils.java:2434-2450) over Game.getDamageDoneThisTurn
// (Game.java:1334-1350): per source, the damage of this turn whose source and
// target copies match, then "Num" (sources), "Max" (largest per-source sum) or
// the total, with "CombatDamage"/"NonCombat" restricting the kind.

// dealFrom resolves a fresh host's DealDamage of n to target for p: every call
// is a new source.
func dealFrom(t *testing.T, g *engine.Game, p engine.PlayerID, target engine.EntityID, n string) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{target})
	resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Any | NumDmg$ "+n)
}

// readCount resolves a GainLife of the Count$ body for who and answers how
// much life it gained, or its error.
func readCount(t *testing.T, g *engine.Game, who engine.PlayerID, body string) (int, error) {
	t.Helper()
	before := g.Player(who).Life
	_, err := castETBChain(t, g, who, etbChainDef(t, "Reader", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$"+body),
		engine.NewScriptedController())
	return g.Player(who).Life - before, err
}

func TestDamageThisTurnCountsPerSource(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	dealFrom(t, g, p, engine.PlayerEntity(other), "2")
	dealFrom(t, g, p, engine.PlayerEntity(other), "3")
	for _, tc := range []struct {
		body string
		want int
	}{
		{"DamageThisTurn Card Player", 5},
		{"NumDamageThisTurn Card Player", 2},
		{"MaxDamageThisTurn Card Player", 3},
		{"NonCombatDamageThisTurn Card Player", 5},
		{"NumCombatDamageThisTurn Card Player", 0},
		{"DamageThisTurn Card You", 0},
		{"NumDamageThisTurn Card Creature.Other", 0},
		{"DamageThisTurn Card Player/Times.2", 10},
	} {
		got, err := readCount(t, g, p, tc.body)
		if err != nil {
			t.Errorf("%s: %v", tc.body, err)
			continue
		}
		if got != tc.want {
			t.Errorf("Count$%s = %d, want %d", tc.body, got, tc.want)
		}
	}
}

// Rith, Liberated Primeval's DragonCheck: a creature an opponent controlled
// was dealt excess damage. The excess flag is the record's own batch outcome
// (CardDamageTable.java:130), not the card's flag of the moment: a first,
// non-lethal batch stays non-excess after a later batch kills with excess, so
// only the later source counts.
func TestDamageThisTurnRithReadsEachBatchsExcess(t *testing.T) {
	t.Parallel()

	const rith = "NumDamageThisTurn Card Creature.OppCtrl+wasDealtExcessDamageThisTurn,Planeswalker.OppCtrl+wasDealtExcessDamageThisTurn"

	t.Run("no excess", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
		dealFrom(t, g, p, engine.CardEntity(victim), "1")
		if got, err := readCount(t, g, p, rith); err != nil || got != 0 {
			t.Errorf("count = %d, %v; want 0", got, err)
		}
	})

	t.Run("excess in a later batch", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
		dealFrom(t, g, p, engine.CardEntity(victim), "1")
		dealFrom(t, g, p, engine.CardEntity(victim), "3")
		if g.Card(victim).Zone != engine.Graveyard {
			t.Fatalf("victim zone = %v, want Graveyard", g.Card(victim).Zone)
		}
		if got, err := readCount(t, g, p, rith); err != nil || got != 1 {
			t.Errorf("count = %d, %v; want 1 (only the excess batch's source)", got, err)
		}
	})

	t.Run("own creature", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		dealFrom(t, g, p, engine.CardEntity(mine), "5")
		if got, err := readCount(t, g, p, rith); err != nil || got != 0 {
			t.Errorf("count = %d, %v; want 0: the creature was not an opponent's", got, err)
		}
	})
}

// Unresolved shapes: the heads Java answers in earlier branches, a body
// without both valid strings, and a player target property the port cannot
// read.
func TestDamageThisTurnUnreadableShapesFail(t *testing.T) {
	t.Parallel()

	for _, body := range []string{
		"TotalDamageThisTurn Card Player",
		"SingleMaxDamageThisTurn",
		"DamageThisTurn Card",
		"DamageThisTurn Card Player.Bogus",
	} {
		g, p, other := newTwoPlayerGame(t)
		dealFrom(t, g, p, engine.PlayerEntity(other), "2")
		if _, err := readCount(t, g, p, body); err == nil {
			t.Errorf("Count$%s resolved, want an error", body)
		}
	}
}

// Count$ExcessDamageReceivedThisTurn is Card.logExcessDamage's running total
// (CardDamageTable.java:142), read off the host or, through
// CardTriggeredTarget$, the damage trigger's target as it last was.
func TestExcessDamageReceivedThisTurn(t *testing.T) {
	t.Parallel()

	t.Run("host", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		resolveLine(t, g, p, engine.NewScriptedController(),
			"DB$ DealDamage | Defined$ Self | NumDmg$ 5 | SubAbility$ DBGain",
			"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$ExcessDamageReceivedThisTurn")
		if got := g.Player(p).Life; got != 24 {
			t.Errorf("life = %d, want 24: 5 damage to a 1/1 is 4 excess", got)
		}
	})

	t.Run("triggered target", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(watcherDef(t, "Electromancer", "Mode$ DamageDone | ValidTarget$ Creature | TriggerZones$ Battlefield",
			"DB$ GainLife | Defined$ You | LifeAmount$ CardTriggeredTarget$ExcessDamageReceivedThisTurn"), p, engine.Battlefield)
		victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		dealFrom(t, g, p, engine.CardEntity(victim), "5")
		if got := g.Player(p).Life; got != 23 {
			t.Errorf("life = %d, want 23: 5 damage to a 2/2 is 3 excess", got)
		}
	})
}
