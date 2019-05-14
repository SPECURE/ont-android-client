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
package at.specure.minidns.client;

import android.content.Context;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.specure.minidns.client.cache.LRUCache;
import at.specure.minidns.client.source.DNSDataSource;
import at.specure.minidns.client.source.NetworkDataSource;
import at.specure.minidns.core.DNSMessage;
import at.specure.minidns.core.DNSName;
import at.specure.minidns.core.Question;
import at.specure.minidns.core.Record;
import at.specure.minidns.core.record.A;
import at.specure.minidns.core.record.AAAA;
import at.specure.minidns.core.record.Data;
import at.specure.minidns.core.record.NS;

/**
 * A minimal DNS client for SRV/A/AAAA/NS and CNAME lookups, with IDN support.
 * This circumvents the missing javax.naming package on android.
 */
public abstract class AbstractDNSClient {

    protected static final LRUCache DEFAULT_CACHE = new LRUCache();

    protected static final Logger LOGGER = Logger.getLogger(AbstractDNSClient.class.getName());

    /**
     * This callback is used by the synchronous query() method <b>and</b> by the asynchronous queryAync() method in order to update the
     * cache. In the asynchronous case, hand this callback into the async call, so that it can get called once the result is available.
     */
    private final DNSDataSource.OnResponseCallback onResponseCallback = new DNSDataSource.OnResponseCallback() {
        @Override
        public void onResponse(DNSMessage requestMessage, DNSMessage responseMessage) {
            final Question q = requestMessage.getQuestion();
            if (cache != null && isResponseCacheable(q, responseMessage)) {
                cache.put(requestMessage.asNormalizedVersion(), responseMessage);
            }
        }
    };

    /**
     * The internal random class for sequence generation.
     */
    protected Random random = null;

    protected Random insecureRandom = new Random();

    /**
     * The internal DNS cache.
     */
    protected DNSCache cache = null;

    protected DNSDataSource dataSource = new NetworkDataSource();
    public Context context;

    public enum IpVersionSetting {

        v4only(true, false),
        v6only(false, true),
        v4v6(true, true),
        v6v4(true, true),;

        public final boolean v4;
        public final boolean v6;

        IpVersionSetting(boolean v4, boolean v6) {
            this.v4 = v4;
            this.v6 = v6;
        }

    }

    protected static IpVersionSetting DEFAULT_IP_VERSION_SETTING = IpVersionSetting.v4v6;

    public static void setDefaultIpVersion(IpVersionSetting preferedIpVersion) {
        if (preferedIpVersion == null) {
            throw new IllegalArgumentException();
        }
        AbstractDNSClient.DEFAULT_IP_VERSION_SETTING = preferedIpVersion;
    }

    protected IpVersionSetting ipVersionSetting = DEFAULT_IP_VERSION_SETTING;

    public void setPreferedIpVersion(IpVersionSetting preferedIpVersion) {
        if (preferedIpVersion == null) {
            throw new IllegalArgumentException();
        }
        ipVersionSetting = preferedIpVersion;
    }

    public IpVersionSetting getPreferedIpVersion() {
        return ipVersionSetting;
    }

    /**
     * Create a new DNS client with the given DNS cache.
     *
     * @param cache The backend DNS cache.
     */
    protected AbstractDNSClient(Context context, DNSCache cache) {
        this.context = context;
        Random random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e1) {
            random = new SecureRandom();
        }
        this.random = random;
        this.cache = cache;
    }

    /**
     * Create a new DNS client using the global default cache.
     */
    protected AbstractDNSClient(Context context) {
        this(context, DEFAULT_CACHE);
    }

    /**
     * Query the system nameservers for a single entry of any class.
     * <p>
     * This can be used to determine the name server version, if name
     * is version.bind, type is TYPE.TXT and clazz is CLASS.CH.
     *
     * @param name  The DNS name to request.
     * @param type  The DNS type to request (SRV, A, AAAA, ...).
     * @param clazz The class of the request (usually IN for Internet).
     * @return The response (or null on timeout/error).
     * @throws IOException if an IO error occurs.
     */
    public final DNSMessage query(String name, Record.TYPE type, Record.CLASS clazz/*, QueryEndListener listener*/) throws IOException {
        Question q = new Question(name, type, clazz);
        return query(q/*, listener*/);
    }

    /**
     * Query the system nameservers for a single entry of the class IN
     * (which is used for MX, SRV, A, AAAA and most other RRs).
     *
     * @param name The DNS name to request.
     * @param type The DNS type to request (SRV, A, AAAA, ...).
     * @return The response (or null on timeout/error).
     * @throws IOException if an IO error occurs.
     */
    public final DNSMessage query(DNSName name, Record.TYPE type/*, QueryEndListener listener*/) throws IOException {
        Question q = new Question(name, type, Record.CLASS.IN);
        return query(q/*, listener*/);
    }

    /**
     * Query the system nameservers for a single entry of the class IN
     * (which is used for MX, SRV, A, AAAA and most other RRs).
     *
     * @param name The DNS name to request.
     * @param type The DNS type to request (SRV, A, AAAA, ...).
     * @return The response (or null on timeout/error).
     * @throws IOException if an IO error occurs.
     */
    public final DNSMessage query(CharSequence name, Record.TYPE type/*, QueryEndListener listener*/) throws IOException {
        Question q = new Question(name, type, Record.CLASS.IN);
        return query(q/*, listener*/);
    }

    public DNSMessage query(Question q/*, QueryEndListener listener*/) throws IOException {
        DNSMessage.Builder query = buildMessage(q);
        return query(query/*, listener*/);
    }

    /**
     * Send a query request to the DNS system.
     *
     * @param query The query to send to the server.
     * @return The response (or null).
     * @throws IOException if an IO error occurs.
     */
    protected abstract DNSMessage query(DNSMessage.Builder query/*, QueryEndListener listener*/) throws IOException;

    public final MiniDnsFuture<DNSMessage, IOException> queryAsync(CharSequence name, Record.TYPE type) {
        Question q = new Question(name, type, Record.CLASS.IN);
        return queryAsync(q);
    }

    public final MiniDnsFuture<DNSMessage, IOException> queryAsync(Question q) {
        DNSMessage.Builder query = buildMessage(q);
        return queryAsync(query);
    }

    /**
     * Default implementation of an asynchronous DNS query which just wraps the synchronous case.
     * <p>
     * Subclasses override this method to support true asynchronous queries.
     * </p>
     *
     * @param query the query.
     * @return a future for this query.
     */
    protected MiniDnsFuture<DNSMessage, IOException> queryAsync(DNSMessage.Builder query) {
        MiniDnsFuture.InternalMiniDnsFuture<DNSMessage, IOException> future = new MiniDnsFuture.InternalMiniDnsFuture<>();
        DNSMessage result;
        try {
            result = query(query);
        } catch (IOException e) {
            future.setException(e);
            return future;
        }
        future.setResult(result);
        return future;
    }

    public final DNSMessage query(Question q, InetAddress server, int port) throws IOException {
        DNSMessage query = getQueryFor(q);
        return query(query, server, port);
    }

    public final DNSMessage query(DNSMessage requestMessage, InetAddress address, int port) throws IOException {
        // See if we have the answer to this question already cached
        DNSMessage responseMessage = (cache == null) ? null : cache.get(requestMessage);
        if (responseMessage != null) {
            return responseMessage;
        }

        final Question q = requestMessage.getQuestion();

        final Level TRACE_LOG_LEVEL = Level.FINE;
        LOGGER.log(TRACE_LOG_LEVEL, "Asking {0} on {1} for {2} with:\n{3}", new Object[]{address, port, q, requestMessage});

        try {
            responseMessage = dataSource.query(requestMessage, address, port);
        } catch (IOException e) {
            LOGGER.log(TRACE_LOG_LEVEL, "IOException {0} on {1} while resolving {2}: {3}", new Object[]{address, port, q, e});
            throw e;
        }
        if (responseMessage != null) {
            LOGGER.log(TRACE_LOG_LEVEL, "Response from {0} on {1} for {2}:\n{3}", new Object[]{address, port, q, responseMessage});
        } else {
            // TODO When should this ever happen?
            LOGGER.log(Level.SEVERE, "NULL response from " + address + " on " + port + " for " + q);
        }

        if (responseMessage == null) return null;

        onResponseCallback.onResponse(requestMessage, responseMessage);

        return responseMessage;
    }

    public final MiniDnsFuture<DNSMessage, IOException> queryAsync(DNSMessage requestMessage, InetAddress address, int port) {
        // See if we have the answer to this question already cached
        DNSMessage responseMessage = (cache == null) ? null : cache.get(requestMessage);
        if (responseMessage != null) {
            return MiniDnsFuture.from(responseMessage);
        }

        final Question q = requestMessage.getQuestion();

        final Level TRACE_LOG_LEVEL = Level.FINE;
        LOGGER.log(TRACE_LOG_LEVEL, "Asynchronusly asking {0} on {1} for {2} with:\n{3}", new Object[]{address, port, q, requestMessage});

        return dataSource.queryAsync(requestMessage, address, port, onResponseCallback);
    }

    /**
     * Whether a response from the DNS system should be cached or not.
     *
     * @param q          The question the response message should answer.
     * @param dnsMessage The response message received using the DNS client.
     * @return True, if the response should be cached, false otherwise.
     */
    protected boolean isResponseCacheable(Question q, DNSMessage dnsMessage) {
        for (Record<? extends Data> record : dnsMessage.answerSection) {
            if (record.isAnswer(q)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a {@link DNSMessage} object carrying the given Question.
     *
     * @param question {@link Question} to be put in the DNS request.
     * @return A {@link DNSMessage} requesting the answer for the given Question.
     */
    final DNSMessage.Builder buildMessage(Question question) {
        DNSMessage.Builder message = DNSMessage.builder();
        message.setQuestion(question);
        message.setId(random.nextInt());
        message = newQuestion(message);
        return message;
    }

    protected abstract DNSMessage.Builder newQuestion(DNSMessage.Builder questionMessage);

    /**
     * Query a nameserver for a single entry.
     *
     * @param name    The DNS name to request.
     * @param type    The DNS type to request (SRV, A, AAAA, ...).
     * @param clazz   The class of the request (usually IN for Internet).
     * @param address The DNS server address.
     * @param port    The DNS server port.
     * @return The response (or null on timeout / failure).
     * @throws IOException On IO Errors.
     */
    public DNSMessage query(String name, Record.TYPE type, Record.CLASS clazz, InetAddress address, int port)
            throws IOException {
        Question q = new Question(name, type, clazz);
        return query(q, address, port);
    }

    /**
     * Query a nameserver for a single entry.
     *
     * @param name    The DNS name to request.
     * @param type    The DNS type to request (SRV, A, AAAA, ...).
     * @param clazz   The class of the request (usually IN for Internet).
     * @param address The DNS server host.
     * @return The response (or null on timeout / failure).
     * @throws IOException On IO Errors.
     */
    public DNSMessage query(String name, Record.TYPE type, Record.CLASS clazz, InetAddress address)
            throws IOException {
        Question q = new Question(name, type, clazz);
        return query(q, address);
    }

    /**
     * Query a nameserver for a single entry of class IN.
     *
     * @param name    The DNS name to request.
     * @param type    The DNS type to request (SRV, A, AAAA, ...).
     * @param address The DNS server host.
     * @return The response (or null on timeout / failure).
     * @throws IOException On IO Errors.
     */
    public DNSMessage query(String name, Record.TYPE type, InetAddress address)
            throws IOException {
        Question q = new Question(name, type, Record.CLASS.IN);
        return query(q, address);
    }

    public final DNSMessage query(DNSMessage query, InetAddress host) throws IOException {
        return query(query, host, 53);
    }

    /**
     * Query a specific server for one entry.
     *
     * @param q       The question section of the DNS query.
     * @param address The dns server address.
     * @return The response (or null on timeout/error).
     * @throws IOException On IOErrors.
     */
    public DNSMessage query(Question q, InetAddress address) throws IOException {
        return query(q, address, 53);
    }

    public final MiniDnsFuture<DNSMessage, IOException> queryAsync(DNSMessage query, InetAddress dnsServer) {
        return queryAsync(query, dnsServer, 53);
    }

    /**
     * Returns the currently used {@link DNSDataSource}. See {@link #setDataSource(DNSDataSource)} for details.
     *
     * @return The currently used {@link DNSDataSource}
     */
    public DNSDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set a {@link DNSDataSource} to be used by the DNSClient.
     * The default implementation will direct all queries directly to the Internet.
     * <p>
     * This can be used to define a non-default handling for outgoing data. This can be useful to redirect the requests
     * to a proxy or to modify requests after or responses before they are handled by the DNSClient implementation.
     *
     * @param dataSource An implementation of DNSDataSource that shall be used.
     */
    public void setDataSource(DNSDataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException();
        }
        this.dataSource = dataSource;
    }

    /**
     * Get the cache used by this DNS client.
     *
     * @return the cached used by this DNS client or <code>null</code>.
     */
    public DNSCache getCache() {
        return cache;
    }

    protected DNSMessage getQueryFor(Question q) {
        DNSMessage.Builder messageBuilder = buildMessage(q);
        DNSMessage query = messageBuilder.build();
        return query;
    }

    private <D extends Data> Set<D> getCachedRecordsFor(DNSName dnsName, Record.TYPE type) {
        Question dnsNameNs = new Question(dnsName, type);
        DNSMessage queryDnsNameNs = getQueryFor(dnsNameNs);
        DNSMessage cachedResult = cache.get(queryDnsNameNs);

        if (cachedResult == null)
            return Collections.emptySet();

        return cachedResult.getAnswersFor(dnsNameNs);
    }

    public Set<NS> getCachedNameserverRecordsFor(DNSName dnsName) {
        return getCachedRecordsFor(dnsName, Record.TYPE.NS);
    }

    public Set<A> getCachedIPv4AddressesFor(DNSName dnsName) {
        return getCachedRecordsFor(dnsName, Record.TYPE.A);
    }

    public Set<AAAA> getCachedIPv6AddressesFor(DNSName dnsName) {
        return getCachedRecordsFor(dnsName, Record.TYPE.AAAA);
    }

    @SuppressWarnings("unchecked")
    private <D extends Data> Set<D> getCachedIPNameserverAddressesFor(DNSName dnsName, Record.TYPE type) {
        Set<NS> nsSet = getCachedNameserverRecordsFor(dnsName);
        if (nsSet.isEmpty())
            return Collections.emptySet();

        Set<D> res = new HashSet<>(3 * nsSet.size());
        for (NS ns : nsSet) {
            Set<D> addresses;
            switch (type) {
                case A:
                    addresses = (Set<D>) getCachedIPv4AddressesFor(ns.target);
                    break;
                case AAAA:
                    addresses = (Set<D>) getCachedIPv6AddressesFor(ns.target);
                    break;
                default:
                    throw new AssertionError();
            }
            res.addAll(addresses);
        }

        return res;
    }

    public Set<A> getCachedIPv4NameserverAddressesFor(DNSName dnsName) {
        return getCachedIPNameserverAddressesFor(dnsName, Record.TYPE.A);
    }

    public Set<AAAA> getCachedIPv6NameserverAddressesFor(DNSName dnsName) {
        return getCachedIPNameserverAddressesFor(dnsName, Record.TYPE.AAAA);
    }
}
