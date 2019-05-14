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
package at.specure.minidns.client.dnsserverlookup;

import java.util.List;

public interface DNSServerLookupMechanism extends Comparable<DNSServerLookupMechanism> {

    public String getName();

    public int getPriority();

    public boolean isAvailable();

    /**
     * Returns a List of String representing ideally IP addresses. The list must be modifiable.
     * <p>
     * Note that the lookup mechanisms are not required to assure that only IP addresses are returned. This verification is performed in
     * when using {@link de.measite.minidns.DNSClient#findDNS()}.
     * </p>
     *
     * @return a List of Strings presenting hopefully IP addresses.
     */
    public List<String> getDnsServerAddresses();

}
