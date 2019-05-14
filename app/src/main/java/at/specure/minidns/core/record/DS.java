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
package at.specure.minidns.core.record;

import java.io.DataInputStream;
import java.io.IOException;

import at.specure.minidns.core.DNSSECConstants;
import at.specure.minidns.core.Record;

/**
 * DS (Delegation Signer) record payload.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4034#section-5">RFC 4034 § 5</a>
 */
public class DS extends DelegatingDnssecRR {

    public static DS parse(DataInputStream dis, int length) throws IOException {
        SharedData parsedData = DelegatingDnssecRR.parseSharedData(dis, length);
        return new DS(parsedData.keyTag, parsedData.algorithm, parsedData.digestType, parsedData.digest);
    }

    public DS(int keyTag, byte algorithm, byte digestType, byte[] digest) {
        super(keyTag, algorithm, digestType, digest);
    }

    public DS(int keyTag, DNSSECConstants.SignatureAlgorithm algorithm, byte digestType, byte[] digest) {
        super(keyTag, algorithm, digestType, digest);
    }

    public DS(int keyTag, DNSSECConstants.SignatureAlgorithm algorithm, DNSSECConstants.DigestAlgorithm digestType, byte[] digest) {
        super(keyTag, algorithm, digestType, digest);
    }

    @Override
    public Record.TYPE getType() {
        return Record.TYPE.DS;
    }

}
