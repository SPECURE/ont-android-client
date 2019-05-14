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

import at.specure.minidns.core.DNSName;
import at.specure.minidns.core.Record;

/**
 * Nameserver record.
 */
public class NS extends RRWithTarget {

    public static NS parse(DataInputStream dis, byte[] data) throws IOException {
        DNSName target = DNSName.parse(dis, data);
        return new NS(target);
    }

    public NS(DNSName name) {
        super(name);
    }

    @Override
    public Record.TYPE getType() {
        return Record.TYPE.NS;
    }

}
