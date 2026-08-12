/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util.perf;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Streams decision traces to a JSON-lines file, one line per decision.
 *
 * <p>The plan's default pass criterion for every optimisation is <em>exact trace identity</em>
 * between a baseline and an optimised build, so the output deliberately contains no timings, no
 * hash codes and no absolute paths: two runs of the same fixture must produce byte-identical files,
 * and {@code diff} must be a sufficient parity check.</p>
 */
public final class DecisionTraceWriter implements PerfSink, Closeable {
    private final Writer writer;
    private final Object lock = new Object();
    private boolean closed;

    public DecisionTraceWriter(final File file) throws IOException {
        final File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
    }

    /** Visible for tests: writes to any sink, not just a file. */
    public DecisionTraceWriter(final Writer writer) {
        this.writer = writer;
    }

    @Override
    public void onDecision(final DecisionRecord record) {
        final List<String> trace = record.getTrace();
        if (trace.isEmpty()) {
            return;
        }
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{\"id\": ").append(record.getId());
        sb.append(", \"kind\": ").append(PerfJson.quote(record.getKind().jsonName()));
        sb.append(", \"player\": ").append(PerfJson.quote(record.getPlayer()));
        sb.append(", \"turn\": ").append(record.getTurn());
        sb.append(", \"phase\": ").append(PerfJson.quote(record.getPhase()));
        sb.append(", \"trace\": [");
        for (int i = 0; i < trace.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(PerfJson.quote(trace.get(i)));
        }
        sb.append("]}").append('\n');

        synchronized (lock) {
            if (closed) {
                return;
            }
            try {
                writer.write(sb.toString());
            } catch (final IOException e) {
                throw new UncheckedIOException("Could not write AI decision trace", e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            writer.close();
        }
    }
}
