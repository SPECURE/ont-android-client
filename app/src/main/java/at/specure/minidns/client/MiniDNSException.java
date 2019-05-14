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
package at.specure.minidns.client;

import java.io.IOException;

import at.specure.minidns.core.DNSMessage;

public abstract class MiniDNSException extends IOException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected MiniDNSException(String message) {
        super(message);
    }

    public static class IdMismatch extends MiniDNSException {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final DNSMessage request;
        private final DNSMessage response;

        public IdMismatch(DNSMessage request, DNSMessage response) {
            super(getString(request, response));
            assert (request.id != response.id);
            this.request = request;
            this.response = response;
        }

        public DNSMessage getRequest() {
            return request;
        }

        public DNSMessage getResponse() {
            return response;
        }

        private static String getString(DNSMessage request, DNSMessage response) {
            return "The response's ID doesn't matches the request ID. Request: " + request.id + ". Response: " + response.id;
        }
    }

    public static class NullResultException extends MiniDNSException {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final DNSMessage request;

        public NullResultException(DNSMessage request) {
            super("The request yielded a 'null' result while resolving.");
            this.request = request;
        }

        public DNSMessage getRequest() {
            return request;
        }
    }
}
