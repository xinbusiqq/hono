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

package org.eclipse.hono.adapter.auth.device;

import java.util.Objects;

import org.eclipse.hono.auth.Device;
import org.eclipse.hono.client.registry.CredentialsClient;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsObject;
import org.springframework.beans.factory.annotation.Autowired;

import io.opentracing.Tracer;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;


/**
 * 使用 Hono 的 <em>Credentials</em> API 验证 X.509 证书的身份验证提供者。
 * An authentication provider that verifies an X.509 certificate using
 * Hono's <em>Credentials</em> API.
 *
 */
public class X509AuthProvider extends CredentialsApiAuthProvider<SubjectDnCredentials> {

    /**
     * 为给定的配置创建一个新的提供者。
     * Creates a new provider for a given configuration.
     *
     * @param credentialsClient The client to use for accessing the Credentials service.
     * @param tracer The tracer instance.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    @Autowired
    public X509AuthProvider(final CredentialsClient credentialsClient, final Tracer tracer) {
        super(credentialsClient, tracer);
    }

    /**
     * 根据设备在其客户端 (X.509) 证书中提供的信息创建一个 {@link SubjectDnCredentials} 实例。
     * Creates a {@link SubjectDnCredentials} instance from information provided by a
     * device in its client (X.509) certificate.
     * <p>
     *     传入的 JSON 对象需要包含一个 <em>subject-dn</em> 和一个 <em>tenant-id</em> 属性。
     * The JSON object passed in is required to contain a <em>subject-dn</em> and a
     * <em>tenant-id</em> property.
     * <p>
     *     如果 JSON 对象不包含 <em>auth-id-template</em> 属性，
     *     则 <em>subject-dn</em> 属性的值用作返回的凭证对象的身份验证标识符。
     *     否则，使用 <em>auth-id-template</em> 生成身份验证标识符。
     * If the JSON object does not contain an <em>auth-id-template</em> property, the value of
     * the <em>subject-dn</em> property is used as the authentication identifier of the returned
     * credentials object. Otherwise, the authentication identifier is generated using
     * the <em>auth-id-template</em>.
     * <p>
     *     JSON 对象中可能存在的任何其他属性都将复制到返回的凭据的客户端上下文中。
     * Any additional properties that might be present in the JSON object
     * are copied into the client context of the returned credentials.
     *
     * @param authInfo 设备提供的凭据。这些通常通过 {@link AuthHandler#parseCredentials(org.eclipse.hono.util.ExecutionContext)} 组装。
     *                 The credentials provided by the device. These usually get assembled via
     *            {@link AuthHandler#parseCredentials(org.eclipse.hono.util.ExecutionContext)}.
     * @return The credentials or {@code null} if the authentication information
     *         does not contain a tenant ID and subject DN.
     * @throws NullPointerException if the authentication info is {@code null}.
     */
    @Override
    public SubjectDnCredentials getCredentials(final JsonObject authInfo) {

        Objects.requireNonNull(authInfo);
        try {
            final String tenantId = authInfo.getString(CredentialsConstants.FIELD_PAYLOAD_TENANT_ID);
            final String subjectDn = authInfo.getString(CredentialsConstants.FIELD_PAYLOAD_SUBJECT_DN);
            final String authIdTemplate = authInfo.getString(CredentialsConstants.FIELD_PAYLOAD_AUTH_ID_TEMPLATE);
            if (tenantId == null || subjectDn == null) {
                return null;
            } else {
                final JsonObject clientContext = authInfo.copy();
                // credentials object already contains tenant ID and subject DN, so remove them from the client context
                clientContext.remove(CredentialsConstants.FIELD_PAYLOAD_TENANT_ID);
                clientContext.remove(CredentialsConstants.FIELD_PAYLOAD_SUBJECT_DN);
                clientContext.remove(CredentialsConstants.FIELD_PAYLOAD_AUTH_ID_TEMPLATE);
                return SubjectDnCredentials.create(tenantId, subjectDn, authIdTemplate, clientContext);
            }
        } catch (final ClassCastException | IllegalArgumentException e) {
            log.warn("Reading authInfo failed", e);
            return null;
        }
    }

    @Override
    protected Future<Device> doValidateCredentials(
            final SubjectDnCredentials deviceCredentials,
            final CredentialsObject credentialsOnRecord) {
        return Future.succeededFuture(new Device(deviceCredentials.getTenantId(), credentialsOnRecord.getDeviceId()));
    }
}
