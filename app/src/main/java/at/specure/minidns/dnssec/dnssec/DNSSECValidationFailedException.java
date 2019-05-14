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

import java.util.List;

import at.specure.minidns.core.Question;
import at.specure.minidns.core.Record;
import at.specure.minidns.core.record.Data;

public class DNSSECValidationFailedException extends RuntimeException {
    private static final long serialVersionUID = 5413184667629832742L;

    public DNSSECValidationFailedException(Question question, String reason) {
        super("Validation of request to " + question + " failed: " + reason);
    }

    public DNSSECValidationFailedException(String message) {
        super(message);
    }

    public DNSSECValidationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DNSSECValidationFailedException(Record<? extends Data> record, String reason) {
        super("Validation of record " + record + " failed: " + reason);
    }

    public DNSSECValidationFailedException(List<Record<? extends Data>> records, String reason) {
        super("Validation of " + records.size() + " " + records.get(0).type + " record" + (records.size() > 1 ? "s" : "") + " failed: " + reason);
    }
}
