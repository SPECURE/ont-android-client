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

import at.specure.minidns.core.EDNS;
import at.specure.minidns.core.util.Hex;

public class UnknownEDNSOption extends EDNSOption {

    protected UnknownEDNSOption(int optionCode, byte[] optionData) {
        super(optionCode, optionData);
    }

    @Override
    public EDNS.OptionCode getOptionCode() {
        return EDNS.OptionCode.UNKNOWN;
    }

    @Override
    protected CharSequence asTerminalOutputInternal() {
        return Hex.from(optionData);
    }

    @Override
    protected CharSequence toStringInternal() {
        return asTerminalOutputInternal();
    }

}
