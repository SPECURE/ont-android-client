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

import java.io.IOException;
import java.lang.reflect.Field;

import at.specure.minidns.client.AbstractDNSClient;
import at.specure.minidns.client.cache.LRUCache;
import at.specure.minidns.integration_test.jul.MiniDnsJul;

public class MiniDnsRepl {

//    public static DNSClient DNSCLIENT = new DNSClient();
//    public static IterativeDNSClient ITERATIVEDNSCLIENT = new IterativeDNSClient();
//    public static DNSSECClient DNSSECCLIENT = new DNSSECClient();

    static {
        LRUCache cache = null;
        try {
            Field defaultCacheField = AbstractDNSClient.class.getDeclaredField("DEFAULT_CACHE");
            defaultCacheField.setAccessible(true);
            cache = (LRUCache) defaultCacheField.get(null);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalStateException(e);
        }
        DEFAULT_CACHE = cache;
    }

    public static final LRUCache DEFAULT_CACHE;

    public static void init() {
        // CHECKSTYLE:OFF
        System.out.println("MiniDNS REPL");
        // CHECKSTYLE:ON
    }

    public static void clearCache() throws SecurityException, IllegalArgumentException {
        DEFAULT_CACHE.clear();
    }

    public static void main(String[] args) throws IOException, SecurityException, IllegalArgumentException {
        MiniDnsJul.enableMiniDnsTrace();

//        ResolverResult<A> res = DnssecResolverApi.INSTANCE.resolveDnssecReliable("verteiltesysteme.net", A.class);
        /*
        DNSSECStats.iterativeDnssecLookupNormalVsExtendedCache();
        DNSSECClient client = new DNSSECClient(new LRUCache(1024));
        DNSSECMessage secRes = client.queryDnssec("verteiltesysteme.net", TYPE.A);
        */

        /*
        DNSSECStats.iterativeDnssecLookupNormalVsExtendedCache();
        NSID nsid = NSIDTest.testNsidLRoot();
        DNSMessage res = RECURSIVEDNSCLIENT.query("mate.geekplace.eu", TYPE.A);
        */
        // CHECKSTYLE:OFF
//        System.out.println(res);
//        System.out.println(nsid);
//      System.out.println(secRes);
//        System.out.println(res);
        // CHCECKSTYLE:ON
    }

}
