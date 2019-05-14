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
package at.specure.minidns.hla.minidns.hla;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.concurrent.TimeoutException;

import at.specure.minidns.client.AbstractDNSClient;
import at.specure.minidns.client.MiniDNSException;
import at.specure.minidns.client.QueryEndListener;
import at.specure.minidns.core.DNSMessage;
import at.specure.minidns.core.DNSName;
import at.specure.minidns.core.Question;
import at.specure.minidns.core.Record;
import at.specure.minidns.core.record.Data;
import at.specure.minidns.core.record.PTR;
import at.specure.minidns.core.record.SRV;
import at.specure.minidns.core.util.InetAddressUtil;
import at.specure.minidns.iterative_resolver.iterative.ReliableDNSClient;

/**
 * The high-level MiniDNS resolving API. It is designed to be easy to use.
 * <p>
 * A simple exammple how to resolve the IPv4 address of a given domain:
 * </p>
 * <pre>
 * {@code
 * ResolverResult<A> result = DnssecResolverApi.INSTANCE.resolve("verteiltesysteme.net", A.class);
 * if (!result.wasSuccessful()) {
 *   RESPONSE_CODE responseCode = result.getResponseCode();
 *   // Perform error handling.
 *   …
 *   return;
 * }
 * if (!result.isAuthenticData()) {
 *   // Response was not secured with DNSSEC.
 *   …
 *   return;
 * }
 * Set<A> answers = result.getAnswers();
 * for (A a : answers) {
 *   InetAddress inetAddress = a.getInetAddress();
 *   // Do someting with the InetAddress, e.g. connect to.
 *   …
 * }
 * }
 * </pre>
 * <p>
 * MiniDNS also supports SRV resource records as first class citizens:
 * </p>
 * <pre>
 * {@code
 * SrvResolverResult result = DnssecResolverApi.INSTANCE.resolveSrv(SrvType.xmpp_client, "example.org")
 * if (!result.wasSuccessful()) {
 *   RESPONSE_CODE responseCode = result.getResponseCode();
 *   // Perform error handling.
 *   …
 *   return;
 * }
 * if (!result.isAuthenticData()) {
 *   // Response was not secured with DNSSEC.
 *   …
 *   return;
 * }
 * List<ResolvedSrvRecord> srvRecords = result.getSortedSrvResolvedAddresses();
 * // Loop over the domain names pointed by the SRV RR. MiniDNS will return the list
 * // correctly sorted by the priority and weight of the related SRV RR.
 * for (ResolvedSrvRecord srvRecord : srvRecord) {
 *   // Loop over the Internet Address RRs resolved for the SRV RR. The order of
 *   // the list depends on the prefered IP version setting of MiniDNS.
 *   for (InternetAddressRR inetAddressRR : srvRecord.addresses) {
 *     InetAddress inetAddress = inetAddressRR.getInetAddress();
 *     int port = srvAddresses.port;
 *     // Try to connect to inetAddress at port.
 *     …
 *   }
 * }
 * }
 * </pre>
 *
 * @author Florian Schmaus
 */
public class ResolverApi {

    private static ResolverApi INSTANCE = null;

    private final AbstractDNSClient dnsClient;

    public ResolverApi(AbstractDNSClient dnsClient) {
        this.dnsClient = dnsClient;
    }

    public final <D extends Data> ResolverResult<D> resolve(String name, Class<D> type) throws IOException {
        return resolve(DNSName.from(name), type);
    }

    public final <D extends Data> ResolverResult<D> resolveAsync(String name, Class<D> type, QueryEndListener listener, long timeout) throws IOException {
        return resolveAsync(DNSName.from(name), type, listener, timeout);
    }

    public final <D extends Data> ResolverResult<D> resolveAsync(DNSName name, Class<D> type, QueryEndListener listener, long timeout) throws IOException {
        Record.TYPE t = Record.TYPE.getType(type);
        Question q = new Question(name, t);
        return resolveAsync(q, listener, timeout);
    }

    public final <D extends Data> ResolverResult<D> resolve(DNSName name, Class<D> type) throws IOException {
        Record.TYPE t = Record.TYPE.getType(type);
        Question q = new Question(name, t);
        return resolve(q);
    }

    public <D extends Data> ResolverResult<D> resolve(Question question) throws IOException {
        DNSMessage dnsMessage = dnsClient.query(question);
        return new ResolverResult<D>(question, dnsMessage, null);
    }

    public <D extends Data> ResolverResult<D> resolveAsync(final Question question, final QueryEndListener listener, final long timeout) throws IOException {

        new AsyncTask<Question, Void, ResolverResult<D>>() {

            private Runnable stopOnTimeout;
            private Handler handler;
            private TimeoutException exception = null;

            @Override
            protected void onPreExecute() {
                handler = new Handler();
                stopOnTimeout = new Runnable() {
                    @Override
                    public void run() {
                        cancel(true);
                        exception = new TimeoutException();
                    }

                    ;
                };
                handler.postDelayed(stopOnTimeout, timeout);
                super.onPreExecute();
            }

            @Override
            protected ResolverResult<D> doInBackground(Question... questions) {
                DNSMessage query = null;
                try {
//                        Thread.sleep(6000);
                    query = dnsClient.query(questions[0]);
                } catch (IOException e) {
                    e.printStackTrace();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
                }

                    /*DNSMessage dnsMessage1 = null;
                    try {
                        dnsMessage1 = dnsMessage.get(10000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }*/
                try {
                    return new ResolverResult<D>(questions[0], query, null);
                } catch (MiniDNSException.NullResultException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(ResolverResult<D> dResolverResult) {
                if (handler != null) {
                    handler.removeCallbacks(stopOnTimeout);
                }
                listener.queryEndListener(dResolverResult.getRawAnswer());
                listener.queryEndListener(dResolverResult, exception);
            }

            @Override
            protected void onCancelled() {
                listener.queryEndListener(null, exception);
                super.onCancelled();
            }
        }.execute(question);

        return null;
    }

    public static ResolverApi getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ResolverApi(new ReliableDNSClient(context));
        }
        return INSTANCE;

    }

    public SrvResolverResult resolveSrv(SrvType type, String serviceName) throws IOException {
        return resolveSrv(type.service, type.proto, DNSName.from(serviceName));
    }

    public SrvResolverResult resolveSrv(SrvType type, DNSName serviceName) throws IOException {
        return resolveSrv(type.service, type.proto, serviceName);
    }

    public SrvResolverResult resolveSrv(SrvService service, SrvProto proto, String name) throws IOException {
        return resolveSrv(service.dnsName, proto.dnsName, DNSName.from(name));
    }

    public SrvResolverResult resolveSrv(SrvService service, SrvProto proto, DNSName name) throws IOException {
        return resolveSrv(service.dnsName, proto.dnsName, name);
    }

    public SrvResolverResult resolveSrv(DNSName service, DNSName proto, DNSName name) throws IOException {
        DNSName srvRrName = DNSName.from(service, proto, name);
        return resolveSrv(srvRrName);
    }

    public SrvResolverResult resolveSrv(String name) throws IOException {
        return resolveSrv(DNSName.from(name));
    }

    public ResolverResult<PTR> reverseLookup(CharSequence inetAddressCs) throws IOException {
        InetAddress inetAddress = InetAddress.getByName(inetAddressCs.toString());
        return reverseLookup(inetAddress);
    }

    public ResolverResult<PTR> reverseLookup(InetAddress inetAddress) throws IOException {
        if (inetAddress instanceof Inet4Address) {
            return reverseLookup((Inet4Address) inetAddress);
        } else if (inetAddress instanceof Inet6Address) {
            return reverseLookup((Inet6Address) inetAddress);
        } else {
            throw new IllegalArgumentException("The given InetAddress '" + inetAddress + "' is neither of type Inet4Address or Inet6Address");
        }
    }

    public ResolverResult<PTR> reverseLookup(Inet4Address inet4Address) throws IOException {
        DNSName reversedIpAddress = InetAddressUtil.reverseIpAddressOf(inet4Address);
        DNSName dnsName = DNSName.from(reversedIpAddress, DNSName.IN_ADDR_ARPA);
        return resolve(dnsName, PTR.class);
    }

    public ResolverResult<PTR> reverseLookup(Inet6Address inet6Address) throws IOException {
        DNSName reversedIpAddress = InetAddressUtil.reverseIpAddressOf(inet6Address);
        DNSName dnsName = DNSName.from(reversedIpAddress, DNSName.IP6_ARPA);
        return resolve(dnsName, PTR.class);
    }

    /**
     * Resolve the {@link SRV} resource record for the given name. After ensuring that the resolution was successful
     * with {@link SrvResolverResult#wasSuccessful()} , and, if DNSSEC was used, that the results could be verified with
     * {@link SrvResolverResult#isAuthenticData()}, simply use {@link SrvResolverResult#getSortedSrvResolvedAddresses()} to
     * retrieve the resolved IP addresses.
     * <p>
     * The name of SRV records is "_[service]._[protocol].[serviceDomain]", for example "_xmpp-client._tcp.example.org".
     * </p>
     *
     * @param name the name to resolve.
     * @return a <code>SrvResolverResult</code> instance which can be used to retrieve the addresses.
     * @throws IOException if an IO exception occurs.
     */
    public SrvResolverResult resolveSrv(DNSName name) throws IOException {
        ResolverResult<SRV> result = resolve(name, SRV.class);
        return new SrvResolverResult(result, this);

    }

    public final AbstractDNSClient getClient() {
        return dnsClient;
    }
}
