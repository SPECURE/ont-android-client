/*
 * Copyright 2015-2018 the original author or authors
 *
 * This software is licensed under the Apache License, Version 2.0,
 * the GNU Lesser General Public License version 2 or later ("LGPL")
 * and the WTFPL.
 * You may choose either license to govern your use of this software only
 * upon the condition that you accept all of the terms of either
 * the Apache License 2.0, the LGPL 2.1+ or the WTFPL.
 */
package at.specure.minidns.iterative_resolver.iterative;

import android.content.Context;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import at.specure.minidns.client.AbstractDNSClient;
import at.specure.minidns.client.DNSCache;
import at.specure.minidns.client.DNSClient;
import at.specure.minidns.client.source.DNSDataSource;
import at.specure.minidns.core.DNSMessage;
import at.specure.minidns.core.Question;
import at.specure.minidns.core.util.MultipleIoException;

/**
 * A DNS client using a reliable strategy. First the configured resolver of the
 * system are used, then, in case there is no answer, a fall back to iterative
 * resolving is performed.
 */
public class ReliableDNSClient extends AbstractDNSClient {

    public enum Mode {
        /**
         * Try the recursive servers first and fallback to iterative resolving if it fails. This is the default mode.
         */
        recursiveWithIterativeFallback,

        /**
         * Only try the recursive servers. This makes {@code ReliableDNSClient} behave like a {@link DNSClient}.
         */
        recursiveOnly,

        /**
         * Only use iterative resolving.  This makes {@code ReliableDNSClient} behave like a {@link IterativeDNSClient}.
         */
        iterativeOnly,
    }

    private IterativeDNSClient recursiveDnsClient = null;
    private DNSClient dnsClient = null;

    private Mode mode = Mode.recursiveWithIterativeFallback;

    public ReliableDNSClient(Context context, DNSCache dnsCache) {
        super(context, dnsCache);
        recursiveDnsClient = new IterativeDNSClient(context, dnsCache) {
            @Override
            protected DNSMessage.Builder newQuestion(DNSMessage.Builder questionMessage) {
                questionMessage = super.newQuestion(questionMessage);
                return ReliableDNSClient.this.newQuestion(questionMessage);
            }

            @Override
            protected boolean isResponseCacheable(Question q, DNSMessage dnsMessage) {
                boolean res = super.isResponseCacheable(q, dnsMessage);
                return ReliableDNSClient.this.isResponseCacheable(q, dnsMessage) && res;
            }
        };
        dnsClient = new DNSClient(context, dnsCache) {
            @Override
            protected DNSMessage.Builder newQuestion(DNSMessage.Builder questionMessage) {
                questionMessage = super.newQuestion(questionMessage);
                return ReliableDNSClient.this.newQuestion(questionMessage);
            }

            @Override
            protected boolean isResponseCacheable(Question q, DNSMessage dnsMessage) {
                boolean res = super.isResponseCacheable(q, dnsMessage);
                return ReliableDNSClient.this.isResponseCacheable(q, dnsMessage) && res;
            }
        };
    }

    public ReliableDNSClient(Context context) {
        this(context, DEFAULT_CACHE);
    }

    @Override
    protected DNSMessage query(final DNSMessage.Builder q/*, QueryEndListener queryEndListener*/) throws IOException {


        final List<IOException> ioExceptions = new LinkedList<>();

        /*AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, DNSMessage>() {
            @Override
            protected DNSMessage doInBackground(Void... voids) {*/
        String unacceptableReason = null;
        DNSMessage dnsMessage = null;

        if (mode != Mode.iterativeOnly) {
            // Try a recursive query.
            try {
                dnsMessage = dnsClient.query(q);
                if (dnsMessage != null) {
                    unacceptableReason = isResponseAcceptable(dnsMessage);
                    if (unacceptableReason == null) {
                        return dnsMessage;
                    }
                }
            } catch (IOException ioException) {
                ioExceptions.add(ioException);
            }
        }

        // Abort if we the are in "recursive only" mode.
        if (mode == Mode.recursiveOnly) return dnsMessage;

        // Eventually log that we fall back to iterative mode.
        final Level FALLBACK_LOG_LEVEL = Level.FINE;
        if (LOGGER.isLoggable(FALLBACK_LOG_LEVEL) && mode != Mode.iterativeOnly) {
            String logString = "Resolution fall back to iterative mode because: ";
            if (!ioExceptions.isEmpty()) {
                logString += ioExceptions.get(0);
            } else if (dnsMessage == null) {
                logString += " DNSClient did not return a response";
            } else if (unacceptableReason != null) {
                logString += unacceptableReason + ". Response:\n" + dnsMessage;
            } else {
                throw new AssertionError("This should never been reached");
            }
            LOGGER.log(FALLBACK_LOG_LEVEL, logString);
        }

        try {
            dnsMessage = recursiveDnsClient.query(q);
        } catch (IOException ioException) {
            ioExceptions.add(ioException);
        }

        if (dnsMessage == null) {
            try {
                MultipleIoException.throwIfRequired(ioExceptions);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       /*         return dnsMessage;
            }

            @Override
            protected void onPostExecute(DNSMessage dnsMessage) {
                queryEndListener(dnsMessage);
            }
        }.execute();*/

        return dnsMessage;
    }

    @Override
    protected DNSMessage.Builder newQuestion(DNSMessage.Builder questionMessage) {
        return questionMessage;
    }

    @Override
    protected boolean isResponseCacheable(Question q, DNSMessage dnsMessage) {
        return isResponseAcceptable(dnsMessage) == null;
    }

    /**
     * Check if the response from the system's nameserver is acceptable. Must return <code>null</code> if the response
     * is acceptable, or a String describing why it is not acceptable. If the response is not acceptable then
     * {@link ReliableDNSClient} will fall back to resolve the query iteratively.
     *
     * @param response the response we got from the system's nameserver.
     * @return <code>null</code> if the response is acceptable, or a String if not.
     */
    protected String isResponseAcceptable(DNSMessage response) {
        return null;
    }

    @Override
    public void setDataSource(DNSDataSource dataSource) {
        super.setDataSource(dataSource);
        recursiveDnsClient.setDataSource(dataSource);
        dnsClient.setDataSource(dataSource);
    }

    /**
     * Set the mode used when resolving queries.
     *
     * @param mode the mode to use.
     */
    public void setMode(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode must not be null.");
        }
        this.mode = mode;
    }
}
