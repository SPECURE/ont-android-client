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
package at.specure.minidns.dnssec.dane;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateEncodingException;

import at.specure.minidns.client.AbstractDNSClient;
import at.specure.minidns.core.DNSMessage;
import at.specure.minidns.core.DNSName;
import at.specure.minidns.core.Record;
import at.specure.minidns.core.record.Data;
import at.specure.minidns.core.record.TLSA;
import at.specure.minidns.dnssec.dnssec.DNSSECClient;
import at.specure.minidns.dnssec.dnssec.DNSSECMessage;
import at.specure.minidns.dnssec.dnssec.UnverifiedReason;

/**
 * A helper class to validate the usage of TLSA records.
 */
public class DaneVerifier {
    private final static Logger LOGGER = Logger.getLogger(DaneVerifier.class.getName());

    private final AbstractDNSClient client;

    public DaneVerifier(Context context) {
        this(new DNSSECClient(context));
    }

    public DaneVerifier(AbstractDNSClient client) {
        this.client = client;
    }

    /**
     * Verifies the certificate chain in an active {@link SSLSocket}. The socket must be connected.
     *
     * @param socket A connected {@link SSLSocket} whose certificate chain shall be verified using DANE.
     * @return Whether the DANE verification is the only requirement according to the TLSA record.
     * If this method returns {@code false}, additional PKIX validation is required.
     * @throws CertificateException if the certificate chain provided differs from the one enforced using DANE.
     */
    public boolean verify(SSLSocket socket) throws CertificateException {
        if (!socket.isConnected()) {
            throw new IllegalStateException("Socket not yet connected.");
        }
        return verify(socket.getSession());
    }

    /**
     * Verifies the certificate chain in an active {@link SSLSession}.
     *
     * @param session An active {@link SSLSession} whose certificate chain shall be verified using DANE.
     * @return Whether the DANE verification is the only requirement according to the TLSA record.
     * If this method returns {@code false}, additional PKIX validation is required.
     * @throws CertificateException if the certificate chain provided differs from the one enforced using DANE.
     */
    public boolean verify(SSLSession session) throws CertificateException {
        try {
            return verifyCertificateChain(convert(session.getPeerCertificateChain()), session.getPeerHost(), session.getPeerPort());
        } catch (SSLPeerUnverifiedException e) {
            throw new CertificateException("Peer not verified", e);
        }
    }

    /**
     * Verifies a certificate chain to be valid when used with the given connection details using DANE.
     *
     * @param chain    A certificate chain that should be verified using DANE.
     * @param hostName The DNS name of the host this certificate chain belongs to.
     * @param port     The port number that was used to reach the server providing the certificate chain in question.
     * @return Whether the DANE verification is the only requirement according to the TLSA record.
     * If this method returns {@code false}, additional PKIX validation is required.
     * @throws CertificateException if the certificate chain provided differs from the one enforced using DANE.
     */
    public boolean verifyCertificateChain(X509Certificate[] chain, String hostName, int port) throws CertificateException {
        DNSName req = DNSName.from("_" + port + "._tcp." + hostName);
        DNSMessage res;
        try {
            res = client.query(req, Record.TYPE.TLSA);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!res.authenticData) {
            String msg = "Got TLSA response from DNS server, but was not signed properly.";
            if (res instanceof DNSSECMessage) {
                msg += " Reasons:";
                for (UnverifiedReason reason : ((DNSSECMessage) res).getUnverifiedReasons()) {
                    msg += " " + reason;
                }
            }
            LOGGER.info(msg);
            return false;
        }

        List<DaneCertificateException.CertificateMismatch> certificateMismatchExceptions = new LinkedList<>();
        boolean verified = false;
        for (Record<? extends Data> record : res.answerSection) {
            if (record.type == Record.TYPE.TLSA && record.name.equals(req)) {
                TLSA tlsa = (TLSA) record.payloadData;
                try {
                    verified |= checkCertificateMatches(chain[0], tlsa, hostName);
                } catch (DaneCertificateException.CertificateMismatch certificateMismatchException) {
                    // Record the mismatch and only throw an exception if no
                    // TLSA RR is able to verify the cert. This allows for TLSA
                    // certificate rollover.
                    certificateMismatchExceptions.add(certificateMismatchException);
                }
                if (verified) break;
            }
        }

        if (!verified && !certificateMismatchExceptions.isEmpty()) {
            throw new DaneCertificateException.MultipleCertificateMismatchExceptions(certificateMismatchExceptions);
        }

        return verified;
    }

    private static boolean checkCertificateMatches(X509Certificate cert, TLSA tlsa, String hostName) throws CertificateException {
        if (tlsa.certUsage == null) {
            LOGGER.warning("TLSA certificate usage byte " + tlsa.certUsageByte + " is not supported while verifying " + hostName);
            return false;
        }

        switch (tlsa.certUsage) {
            case serviceCertificateConstraint:
            case domainIssuedCertificate:
                break;
            case caConstraint:
            case trustAnchorAssertion:
            default:
                LOGGER.warning("TLSA certificate usage " + tlsa.certUsage + " (" + tlsa.certUsageByte + ") not supported while verifying " + hostName);
                return false;
        }

        if (tlsa.selector == null) {
            LOGGER.warning("TLSA selector byte " + tlsa.selectorByte + " is not supported while verifying " + hostName);
            return false;
        }

        byte[] comp = null;
        switch (tlsa.selector) {
            case fullCertificate:
                comp = cert.getEncoded();
                break;
            case subjectPublicKeyInfo:
                comp = cert.getPublicKey().getEncoded();
                break;
            default:
                LOGGER.warning("TLSA selector " + tlsa.selector + " (" + tlsa.selectorByte + ") not supported while verifying " + hostName);
                return false;
        }

        if (tlsa.matchingType == null) {
            LOGGER.warning("TLSA matching type byte " + tlsa.matchingTypeByte + " is not supported while verifying " + hostName);
            return false;
        }

        switch (tlsa.matchingType) {
            case noHash:
                break;
            case sha256:
                try {
                    comp = MessageDigest.getInstance("SHA-256").digest(comp);
                } catch (NoSuchAlgorithmException e) {
                    throw new CertificateException("Verification using TLSA failed: could not SHA-256 for matching", e);
                }
                break;
            case sha512:
                try {
                    comp = MessageDigest.getInstance("SHA-512").digest(comp);
                } catch (NoSuchAlgorithmException e) {
                    throw new CertificateException("Verification using TLSA failed: could not SHA-512 for matching", e);
                }
                break;
            default:
                LOGGER.warning("TLSA matching type " + tlsa.matchingType + " not supported while verifying " + hostName);
                return false;
        }

        boolean matches = tlsa.certificateAssociationEquals(comp);
        if (!matches) {
            throw new DaneCertificateException.CertificateMismatch(tlsa, comp);
        }

        // domain issued certificate does not require further verification,
        // service certificate constraint does.
        return tlsa.certUsage == TLSA.CertUsage.domainIssuedCertificate;
    }

    /**
     * Invokes {@link HttpsURLConnection#connect()} in a DANE verified fashion.
     * This method must be called before {@link HttpsURLConnection#connect()} is invoked.
     * <p>
     * If a SSLSocketFactory was set on this HttpsURLConnection, it will be ignored. You can use
     * {@link #verifiedConnect(HttpsURLConnection, X509TrustManager)} to inject a custom {@link TrustManager}.
     *
     * @param conn connection to be connected.
     * @return The {@link HttpsURLConnection} after being connected.
     * @throws IOException          when the connection could not be established.
     * @throws CertificateException if there was an exception while verifying the certificate.
     */
    public HttpsURLConnection verifiedConnect(HttpsURLConnection conn) throws IOException, CertificateException {
        return verifiedConnect(conn, null);
    }

    /**
     * Invokes {@link HttpsURLConnection#connect()} in a DANE verified fashion.
     * This method must be called before {@link HttpsURLConnection#connect()} is invoked.
     * <p>
     * If a SSLSocketFactory was set on this HttpsURLConnection, it will be ignored.
     *
     * @param conn         connection to be connected.
     * @param trustManager A non-default {@link TrustManager} to be used.
     * @return The {@link HttpsURLConnection} after being connected.
     * @throws IOException          when the connection could not be established.
     * @throws CertificateException if there was an exception while verifying the certificate.
     */
    public HttpsURLConnection verifiedConnect(HttpsURLConnection conn, X509TrustManager trustManager) throws IOException, CertificateException {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            ExpectingTrustManager expectingTrustManager = new ExpectingTrustManager(trustManager);
            context.init(null, new TrustManager[]{expectingTrustManager}, null);
            conn.setSSLSocketFactory(context.getSocketFactory());
            conn.connect();
            boolean fullyVerified = verifyCertificateChain(convert(conn.getServerCertificates()), conn.getURL().getHost(),
                    conn.getURL().getPort() < 0 ? conn.getURL().getDefaultPort() : conn.getURL().getPort());
            // If fullyVerified is true then it's the DANE verification performed by verifiyCertificateChain() is
            // sufficient to verify the certificate and we ignore possible pending exceptions of ExpectingTrustManager.
            if (!fullyVerified && expectingTrustManager.hasException()) {
                throw new IOException("Peer verification failed using PKIX", expectingTrustManager.getException());
            }
            return conn;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static X509Certificate[] convert(Certificate[] certificates) {
        List<X509Certificate> certs = new ArrayList<>();
        for (Certificate certificate : certificates) {
            if (certificate instanceof X509Certificate) {
                certs.add((X509Certificate) certificate);
            }
        }
        return certs.toArray(new X509Certificate[certs.size()]);
    }

    private static X509Certificate[] convert(javax.security.cert.X509Certificate[] certificates) {
        X509Certificate[] certs = new X509Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            try {
                certs[i] = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificates[i].getEncoded()));
            } catch (CertificateException | CertificateEncodingException e) {
                LOGGER.log(Level.WARNING, "Could not convert", e);
            }
        }
        return certs;
    }
}
