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
package at.specure.minidns.minidnsrepl;

import android.content.Context;

import java.io.IOException;
import java.util.Arrays;

import at.specure.minidns.client.AbstractDNSClient;
import at.specure.minidns.client.DNSCache;
import at.specure.minidns.client.source.NetworkDataSourceWithAccounting;
import at.specure.minidns.core.DNSMessage;
import at.specure.minidns.core.Record;
import at.specure.minidns.dnssec.dnssec.DNSSECClient;
import at.specure.minidns.integration_test.integrationtest.IntegrationTestTools;

import static java.lang.System.out;

public class MiniDNSStats {

    public static void main(String[] args) throws IOException {
//        showDnssecStats();
    }

    public static void showDnssecStats(Context context) throws IOException {
        showDnssecStats(context, "siccegge.de", Record.TYPE.A);
    }

    public static void showDnssecStats(Context context, String name, Record.TYPE type) throws IOException {
        DNSSECClient client;

        client = getClient(context, IntegrationTestTools.CacheConfig.without);
        // CHECKSTYLE:OFF
        out.println(gatherStatsFor(client, "Without Cache", name, type));
        // CHECKSTYLE:ON

        client = getClient(context, IntegrationTestTools.CacheConfig.normal);
        // CHECKSTYLE:OFF
        out.println(gatherStatsFor(client, "With Cache", name, type));
        // CHECKSTYLE:ON

        client = getClient(context, IntegrationTestTools.CacheConfig.extended);
        // CHECKSTYLE:OFF
        out.println(gatherStatsFor(client, "With Extended Cache", name, type));
        // CHECKSTYLE:ON

        client = getClient(context, IntegrationTestTools.CacheConfig.full);
        // CHECKSTYLE:OFF
        out.println(gatherStatsFor(client, "With Full Cache", name, type));
        // CHECKSTYLE:ON
    }

    public static StringBuilder gatherStatsFor(DNSSECClient client, String testName, String name, Record.TYPE type) throws IOException {
        DNSMessage response;
        long start, stop;

        start = System.currentTimeMillis();
        response = client.query(name, type);
        stop = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append(testName).append('\n');
        char[] headline = new char[testName.length()];
        Arrays.fill(headline, '#');
        sb.append(headline).append('\n');
        sb.append(response).append('\n');
        sb.append("Took ").append(stop - start).append("ms").append('\n');
        sb.append(getStats(client)).append('\n');
        sb.append('\n');

        return sb;
    }

    public static DNSSECClient getClient(Context context, IntegrationTestTools.CacheConfig cacheConfig) {
        return IntegrationTestTools.getClient(context, cacheConfig);
    }

    public static StringBuilder getStats(AbstractDNSClient client) {
        StringBuilder sb = new StringBuilder();

        NetworkDataSourceWithAccounting ndswa = NetworkDataSourceWithAccounting.from(client);
        if (ndswa != null) {
            sb.append(ndswa.getStats().toString());
        } else {
            sb.append("Client is not using " + NetworkDataSourceWithAccounting.class.getSimpleName());
        }

        DNSCache dnsCache = client.getCache();
        if (dnsCache != null) {
            sb.append(dnsCache);
        } else {
            sb.append("Client is not using a Cache");
        }

        return sb;
    }
}
