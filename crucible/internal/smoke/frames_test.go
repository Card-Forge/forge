package smoke

import (
	"strings"
	"testing"
)

// Internal test (TEST-2): the recovered-panic path of Probe cannot be reached
// from outside without an engine bug to trigger it, so the trimming of its
// stack is tested on its own.
func TestFirstFramesKeepsTheTopOfAStack(t *testing.T) {
	t.Parallel()

	var stack []string
	stack = append(stack, "goroutine 1 [running]:")
	for range 20 {
		stack = append(stack, "pkg.fn(...)", "\tfile.go:1")
	}
	got := firstFrames([]byte(strings.Join(stack, "\n")), 3)
	if lines := strings.Count(got, "\n") + 1; lines != 7 {
		t.Errorf("kept %d lines, want the header and 3 frames (7)", lines)
	}
	if short := firstFrames([]byte("one line"), 3); short != "one line" {
		t.Errorf("a short stack changed: %q", short)
	}
}
