/*******************************************************************************
 * Copyright (c) 2016, 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.hono.service.auth;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import io.vertx.core.Future;

/**
 * 用于验证证书路径的函数。
 * A function for validating certificate paths.
 *
 */
public interface X509CertificateChainValidator {

    /**
     * 根据信任锚验证证书路径。
     * Validates a certificate path based on a trust anchor.
     *
     * @param chain 要验证的证书链。 结束证书必须位于位置 0。
     *              The certificate chain to validate. The end certificate
     *              must be at position 0.
     * @param trustAnchor 用于验证链的信任锚。
     *                    The trust anchor to use for validating the chain.
     * @return 如果路径有效（根据已实施的测试），则为完成的future。 否则，future 将因 {@link java.security.cert.CertificateException} 而失败。
     *         A completed future if the path is valid (according to the implemented tests).
     *         Otherwise, the future will be failed with a {@link java.security.cert.CertificateException}.
     * @throws NullPointerException if any of the parameters are {@code null}.
     * @throws IllegalArgumentException if the chain is empty.
     */
    Future<Void> validate(List<X509Certificate> chain, TrustAnchor trustAnchor);

    /**
     * 根据信任锚列表验证证书路径。
     * Validates a certificate path based on a list of trust anchors.
     *
     * @param chain 要验证的证书链。 结束证书必须位于位置 0。
     *              The certificate chain to validate. The end certificate
     *              must be at position 0.
     * @param trustAnchors 用于验证链的信任锚。
     *                     The list of trust anchors to use for validating the chain.
     * @return 如果路径有效（根据已实施的测试），则为完成的future。 否则，future 将因 {@link java.security.cert.CertificateException} 而失败。
     *         A completed future if the path is valid (according to the implemented tests).
     *         Otherwise, the future will be failed with a {@link java.security.cert.CertificateException}.
     * @throws NullPointerException if any of the parameters are {@code null}.
     * @throws IllegalArgumentException if the chain or trust anchor list are empty.
     */
    Future<Void> validate(List<X509Certificate> chain, Set<TrustAnchor> trustAnchors);
}
