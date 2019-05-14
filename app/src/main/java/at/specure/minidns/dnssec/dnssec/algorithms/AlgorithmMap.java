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
package at.specure.minidns.dnssec.dnssec.algorithms;


import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.specure.minidns.core.DNSSECConstants;
import at.specure.minidns.core.record.NSEC3;
import at.specure.minidns.dnssec.dnssec.DNSSECValidatorInitializationException;
import at.specure.minidns.dnssec.dnssec.DigestCalculator;
import at.specure.minidns.dnssec.dnssec.SignatureVerifier;

public class AlgorithmMap {
    private Logger LOGGER = Logger.getLogger(AlgorithmMap.class.getName());

    public static final AlgorithmMap INSTANCE = new AlgorithmMap();

    private final Map<DNSSECConstants.DigestAlgorithm, DigestCalculator> dsDigestMap = new HashMap<>();
    private final Map<DNSSECConstants.SignatureAlgorithm, SignatureVerifier> signatureMap = new HashMap<>();
    private final Map<NSEC3.HashAlgorithm, DigestCalculator> nsecDigestMap = new HashMap<>();

    @SuppressWarnings("deprecation")
    private AlgorithmMap() {
        try {
            dsDigestMap.put(DNSSECConstants.DigestAlgorithm.SHA1, new JavaSecDigestCalculator("SHA-1"));
            nsecDigestMap.put(NSEC3.HashAlgorithm.SHA1, new JavaSecDigestCalculator("SHA-1"));
        } catch (NoSuchAlgorithmException e) {
            // SHA-1 is MANDATORY
            throw new DNSSECValidatorInitializationException("SHA-1 is mandatory", e);
        }
        try {
            dsDigestMap.put(DNSSECConstants.DigestAlgorithm.SHA256, new JavaSecDigestCalculator("SHA-256"));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is MANDATORY
            throw new DNSSECValidatorInitializationException("SHA-256 is mandatory", e);
        }

        try {
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.RSAMD5, new RSASignatureVerifier("MD5withRSA"));
        } catch (NoSuchAlgorithmException e) {
            // RSA/MD5 is DEPRECATED
            LOGGER.log(Level.FINER, "Platform does not support RSA/MD5", e);
        }
        try {
            DSASignatureVerifier sha1withDSA = new DSASignatureVerifier("SHA1withDSA");
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.DSA, sha1withDSA);
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.DSA_NSEC3_SHA1, sha1withDSA);
        } catch (NoSuchAlgorithmException e) {
            // DSA/SHA-1 is OPTIONAL
            LOGGER.log(Level.FINE, "Platform does not support DSA/SHA-1", e);
        }
        try {
            RSASignatureVerifier sha1withRSA = new RSASignatureVerifier("SHA1withRSA");
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.RSASHA1, sha1withRSA);
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.RSASHA1_NSEC3_SHA1, sha1withRSA);
        } catch (NoSuchAlgorithmException e) {
            throw new DNSSECValidatorInitializationException("Platform does not support RSA/SHA-1", e);
        }
        try {
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.RSASHA256, new RSASignatureVerifier("SHA256withRSA"));
        } catch (NoSuchAlgorithmException e) {
            // RSA/SHA-256 is RECOMMENDED
            LOGGER.log(Level.INFO, "Platform does not support RSA/SHA-256", e);
        }
        try {
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.RSASHA512, new RSASignatureVerifier("SHA512withRSA"));
        } catch (NoSuchAlgorithmException e) {
            // RSA/SHA-512 is RECOMMENDED
            LOGGER.log(Level.INFO, "Platform does not support RSA/SHA-512", e);
        }
        try {
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.ECC_GOST, new ECGOSTSignatureVerifier());
        } catch (NoSuchAlgorithmException e) {
            // GOST R 34.10-2001 is OPTIONAL
            LOGGER.log(Level.FINE, "Platform does not support GOST R 34.10-2001", e);
        }
        try {
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.ECDSAP256SHA256, new ECDSASignatureVerifier.P256SHA256());
        } catch (NoSuchAlgorithmException e) {
            // ECDSA/SHA-256 is RECOMMENDED
            LOGGER.log(Level.INFO, "Platform does not support ECDSA/SHA-256", e);
        }
        try {
            signatureMap.put(DNSSECConstants.SignatureAlgorithm.ECDSAP384SHA384, new ECDSASignatureVerifier.P384SHA284());
        } catch (NoSuchAlgorithmException e) {
            // ECDSA/SHA-384 is RECOMMENDED
            LOGGER.log(Level.INFO, "Platform does not support ECDSA/SHA-384", e);
        }
    }

    public DigestCalculator getDsDigestCalculator(DNSSECConstants.DigestAlgorithm algorithm) {
        return dsDigestMap.get(algorithm);
    }

    public SignatureVerifier getSignatureVerifier(DNSSECConstants.SignatureAlgorithm algorithm) {
        return signatureMap.get(algorithm);
    }

    public DigestCalculator getNsecDigestCalculator(NSEC3.HashAlgorithm algorithm) {
        return nsecDigestMap.get(algorithm);
    }
}
