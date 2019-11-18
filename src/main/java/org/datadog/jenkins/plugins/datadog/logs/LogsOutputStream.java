package org.datadog.jenkins.plugins.datadog.logs;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;


public class LogsOutputStream extends LineTransformationOutputStream {
    private final OutputStream delegate;
    private final LogsWriter datadoglogs;

    public LogsOutputStream(OutputStream delegate, LogsWriter datadoglogs) {
        super();
        this.delegate = delegate;
        this.datadoglogs = datadoglogs;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        delegate.write(b, 0, len);
        this.flush();

        if(!datadoglogs.isConnectionBroken()) {
            String line = new String(b, 0, len, datadoglogs.getCharset());
            line = ConsoleNote.removeNotes(line).trim();
            datadoglogs.write(Collections.singletonList(line));
        }
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        super.close();
    }
}
