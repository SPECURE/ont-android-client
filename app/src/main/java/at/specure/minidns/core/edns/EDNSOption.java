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
package at.specure.minidns.core.edns;

import java.io.DataOutputStream;
import java.io.IOException;

import at.specure.minidns.core.EDNS;


public abstract class EDNSOption {

    public final int optionCode;
    public final int optionLength;

    protected final byte[] optionData;

    protected EDNSOption(int optionCode, byte[] optionData) {
        this.optionCode = optionCode;
        this.optionLength = optionData.length;
        this.optionData = optionData;
    }

    protected EDNSOption(byte[] optionData) {
        this.optionCode = getOptionCode().asInt;
        this.optionLength = optionData.length;
        this.optionData = optionData;
    }

    public final void writeToDos(DataOutputStream dos) throws IOException {
        dos.writeShort(optionCode);
        dos.writeShort(optionLength);
        dos.write(optionData);
    }

    public abstract EDNS.OptionCode getOptionCode();

    private String toStringCache;

    @Override
    public final String toString() {
        if (toStringCache == null) {
            toStringCache = toStringInternal().toString();
        }
        return toStringCache;
    }

    protected abstract CharSequence toStringInternal();

    private String terminalOutputCache;

    public final String asTerminalOutput() {
        if (terminalOutputCache == null) {
            terminalOutputCache = asTerminalOutputInternal().toString();
        }
        return terminalOutputCache;
    }

    protected abstract CharSequence asTerminalOutputInternal();

    public static EDNSOption parse(int intOptionCode, byte[] optionData) {
        EDNS.OptionCode optionCode = EDNS.OptionCode.from(intOptionCode);
        EDNSOption res;
        switch (optionCode) {
            case NSID:
                res = new NSID(optionData);
                break;
            default:
                res = new UnknownEDNSOption(intOptionCode, optionData);
                break;
        }
        return res;
    }

}
