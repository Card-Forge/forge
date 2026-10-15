package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

const openPlan = "docs/crucible/00-master-implementation-plan-open.md"

// planRm deletes the open-plan table rows whose first cell is one of ids.
func planRm(root string, ids []string) error {
	p := filepath.Join(root, openPlan)
	b, err := os.ReadFile(p)
	if err != nil {
		return err
	}
	lines := strings.Split(string(b), "\n")
	for _, id := range ids {
		i := rowIndex(lines, id)
		if i < 0 {
			return fmt.Errorf("open plan has no row %s", id)
		}
		lines = append(lines[:i], lines[i+1:]...)
	}
	return os.WriteFile(p, []byte(strings.Join(lines, "\n")), 0o644)
}

// planAdd inserts a new row after the row `after`.
func planAdd(root, after, id, text, phase, size, dep string) error {
	if strings.ContainsAny(text, "|\n") {
		return errors.New("row text must not contain | or a newline")
	}
	p := filepath.Join(root, openPlan)
	b, err := os.ReadFile(p)
	if err != nil {
		return err
	}
	lines := strings.Split(string(b), "\n")
	if rowIndex(lines, id) >= 0 {
		return fmt.Errorf("open plan already has row %s", id)
	}
	i := rowIndex(lines, after)
	if i < 0 {
		return fmt.Errorf("open plan has no row %s", after)
	}
	row := fmt.Sprintf("| %s | %s | %s | %s | %s |", id, text, phase, size, dep)
	lines = append(lines[:i+1], append([]string{row}, lines[i+1:]...)...)
	return os.WriteFile(p, []byte(strings.Join(lines, "\n")), 0o644)
}

// planRow returns the text cell of row id.
func planRow(root, id string) (string, error) {
	b, err := os.ReadFile(filepath.Join(root, openPlan))
	if err != nil {
		return "", err
	}
	lines := strings.Split(string(b), "\n")
	i := rowIndex(lines, id)
	if i < 0 {
		return "", fmt.Errorf("open plan has no row %s", id)
	}
	cells := strings.Split(lines[i], "|")
	if len(cells) < 4 {
		return "", fmt.Errorf("row %s is not a table row", id)
	}
	return strings.Join(strings.Fields(cells[2]), " "), nil
}

func rowIndex(lines []string, id string) int {
	for i, l := range lines {
		if f := strings.Fields(l); len(f) >= 2 && f[0] == "|" && f[1] == id {
			return i
		}
	}
	return -1
}
