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
package at.specure.minidns.dnssec.dnssec;

import java.util.Collections;
import java.util.Set;

import at.specure.minidns.core.DNSMessage;
import at.specure.minidns.core.Record;
import at.specure.minidns.core.record.RRSIG;

public class DNSSECMessage extends DNSMessage {
    private final Set<Record<RRSIG>> signatures;
    private final Set<UnverifiedReason> result;

    DNSSECMessage(Builder copy, Set<Record<RRSIG>> signatures, Set<UnverifiedReason> unverifiedReasons) {
        super(copy.setAuthenticData(unverifiedReasons == null || unverifiedReasons.isEmpty()));
        this.signatures = Collections.unmodifiableSet(signatures);
        this.result = unverifiedReasons == null ? Collections.<UnverifiedReason>emptySet() : Collections.unmodifiableSet(unverifiedReasons);
    }

    public Set<Record<RRSIG>> getSignatures() {
        return signatures;
    }

    public Set<UnverifiedReason> getUnverifiedReasons() {
        return result;
    }

}
