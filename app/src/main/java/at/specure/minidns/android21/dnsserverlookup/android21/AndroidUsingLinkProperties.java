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
package at.specure.minidns.android21.dnsserverlookup.android21;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import at.specure.minidns.client.dnsserverlookup.AbstractDNSServerLookupMechanism;
import at.specure.minidns.client.dnsserverlookup.AndroidUsingExec;
import at.specure.minidns.client.dnsserverlookup.DNSServerLookupMechanism;


/**
 * Requires the ACCESS_NETWORK_STATE permission.
 */
public class AndroidUsingLinkProperties extends AbstractDNSServerLookupMechanism {

    private final ConnectivityManager connectivityManager;
    private static DNSServerLookupMechanism INSTANCE = null;

    public static DNSServerLookupMechanism getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AndroidUsingLinkProperties(context);
        }
        return INSTANCE;
    }


    protected AndroidUsingLinkProperties(Context context) {
        super(AndroidUsingLinkProperties.class.getSimpleName(), AndroidUsingExec.PRIORITY - 1);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    @TargetApi(21)
    public List<String> getDnsServerAddresses() {
        Network[] networks = connectivityManager.getAllNetworks();
        if (networks == null) {
            return null;
        }

        List<String> servers = new ArrayList<>(networks.length * 2);
        for (Network network : networks) {
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            if (linkProperties == null) {
                continue;
            }

            // Prioritize the DNS servers of links which have a default route
            if (hasDefaultRoute(linkProperties)) {
                servers.addAll(0, toListOfStrings(linkProperties.getDnsServers()));
            } else {
                servers.addAll(toListOfStrings(linkProperties.getDnsServers()));
            }
        }

        if (servers.isEmpty()) {
            return null;
        }

        return servers;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean hasDefaultRoute(LinkProperties linkProperties) {
        for (RouteInfo route : linkProperties.getRoutes()) {
            if (route.isDefaultRoute()) {
                return true;
            }
        }
        return false;
    }

}
