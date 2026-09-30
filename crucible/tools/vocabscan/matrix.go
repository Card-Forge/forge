// The parity matrix, generated.

package main

import (
	"bufio"
	"fmt"
	"io"

	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// matrixRow is one vocabulary's row. `defined` is what Java declares, where
// that is a countable thing; `supported` is what the Go engine implements, and
// only the API row can answer it until the vocabularies behind the others have
// registries of their own.
type matrixRow struct {
	kind    vocab.Kind
	label   string
	defined string
	// supported is filled from the engine for the API row and left empty for
	// the rest, because a zero would read as a measurement and it is not one.
	supported string
	note      string
}

// rows is the matrix, in the order the document lists it.
var rows = []matrixRow{
	{kind: vocab.API, label: "Ability API (`ApiType`)", defined: "202"},
	{kind: vocab.ParamKey, label: "Ability param key", defined: "—"},
	{kind: vocab.KeywordHead, label: "Keyword head", defined: "203", note: "keyword"},
	{kind: vocab.Mode, label: "Trigger and static mode", defined: "153", note: "mode"},
	{kind: vocab.ReplacementEvent, label: "Replacement event", defined: "45"},
	{kind: vocab.CostPart, label: "Cost part", defined: "~45"},
	{kind: vocab.CountHead, label: "Count head", defined: "—"},
	{kind: vocab.CountOperator, label: "Count operator", defined: "—"},
	{kind: vocab.SVarHead, label: "Amount-expression head", defined: "—"},
	{kind: vocab.SVarProperty, label: "Amount-expression property", defined: "—"},
	{kind: vocab.ValidBase, label: "Valid base", defined: "—"},
	{kind: vocab.ValidProperty, label: "Valid property", defined: "—"},
	{kind: vocab.AIHintKey, label: "AI hint key", defined: "—"},
}

// writeMatrix writes docs/crucible/porting/parity-matrix.md in full.
//
// The whole file, prose included, so "generated, do not hand-edit" is true
// rather than aspirational. A half-generated document is one somebody edits by
// hand in the half that looks safe.
func writeMatrix(w io.Writer, v *vocab.Vocabulary, cards int) error {
	var reg engine.Registry
	out := bufio.NewWriter(w)

	_, _ = fmt.Fprintf(out, `# Parity Matrix

- **Status:** Generated. Do not hand-edit — regenerate with
  `+"`go run ./tools/vocabscan -matrix > ../docs/crucible/porting/parity-matrix.md`"+` and then
  `+"`prettier --write`"+`, which owns the table alignment (DOC-14)

Support status of every card-script vocabulary item in the Go engine, over %s cards.

The used column is what the corpus writes. The supported column is what the engine implements, and only the API row can
answer it: the other vocabularies are consumed by code that has no registry to count yet, and a zero there would read as
a measurement rather than an absence.

| Kind | Defined in Java | Used in scripts | Supported in Go |
| ---- | --------------: | --------------: | --------------: |
`, thousands(cards))

	for _, r := range rows {
		supported := r.supported
		if r.kind == vocab.API {
			supported = fmt.Sprintf("%d", reg.Implemented())
		}
		if supported == "" {
			supported = "—"
		}
		_, _ = fmt.Fprintf(out, "| %s | %s | %s | %s |\n",
			r.label, r.defined, thousands(v.Distinct(r.kind)), supported)
	}

	_, _ = fmt.Fprintf(out, `
Two rows read higher than their Java definition count because the script vocabulary is not the enum: keyword heads
include the ones `+"`CardFactoryUtil`"+` expands without a `+"`Keyword`"+` constant, and modes count triggers and statics together
because the ones defined on an SVar body carry nothing saying which the referencing line is.

## Deliberate exclusions

Read by `+"`tools/apiscan -check`"+`, which fails on any param key nothing reads that is not listed here. The gate matches on
both the item and its kind, so a row cannot silence a token of a different vocabulary by accident.

| Item | Kind | Reason | Revisit |
| ---- | ---- | ------ | ------- |
| —    | —    | none   | —       |

**Empty, and that is the target.** An exclusion is not "this token is fine"; it is "this token fails the gate and
someone decided to ship anyway". Every dead param the scan found was fixed instead —
[card-script-defects.md](card-script-defects.md).

## Both questions, both blocking

`+"`tools/apiscan -check`"+` proves "some Java code reads this key". `+"`-check -api`"+` proves the stronger claim — "the effect this
card names reads this key" — by attributing keys per effect class and following each class's superclass chain inside the
effects directory. Both run in CI and both fail the build.

`+"`-api`"+` also follows one level of static helper calls into another effect class (`+"`addHelperParams`, `scan.go`"+`), reading
only the called method's body. Reason: `+"`CloneEffect`"+` reads `+"`PumpDuration$`"+` solely through
`+"`TokenEffectBase.addPumpUntil(sa, ...)`"+` without extending `+"`TokenEffectBase`"+`, so the superclass chain alone reported
`+"`the_fourteenth_doctor.txt`'s `PumpDuration$`"+` as dead once `+"`compile`"+` began compiling `+"`ETBReplacement:Copy`"+` SVars.
`)
	return out.Flush()
}

// thousands formats a count the way the document does, so a regenerated file
// does not differ from the committed one by punctuation alone.
func thousands(n int) string {
	if n < 1000 {
		return fmt.Sprintf("%d", n)
	}
	return fmt.Sprintf("%d,%03d", n/1000, n%1000)
}
