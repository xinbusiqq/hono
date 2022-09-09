/*******************************************************************************
 * Copyright (c) 2016, 2020 Contributors to the Eclipse Foundation
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

import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.Objects;

import org.eclipse.hono.auth.Device;
import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.client.ClientErrorException;
import org.eclipse.hono.client.registry.CredentialsClient;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsObject;
import org.eclipse.hono.util.JsonHelper;
import org.springframework.beans.factory.annotation.Autowired;

import io.opentracing.Tracer;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * An authentication provider that verifies username/password credentials using
 * Hono's <em>Credentials</em> API.
 */
public final class UsernamePasswordAuthProvider extends CredentialsApiAuthProvider<UsernamePasswordCredentials> {

    private final HonoPasswordEncoder pwdEncoder;

    /**
     * 为给定的配置创建一个新的提供者。
     * Creates a new provider for a given configuration.
     *
     * @param credentialsClient The client to use for accessing the Credentials service.
     * @param tracer The tracer instance.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    @Autowired
    public UsernamePasswordAuthProvider(final CredentialsClient credentialsClient, final Tracer tracer) {
        this(credentialsClient, new SpringBasedHonoPasswordEncoder(), tracer);
    }

    /**
     * 为给定的配置创建一个新的提供者。
     * Creates a new provider for a given configuration.
     *
     * @param credentialsClient 用于访问凭据服务的客户端。
     *                          The client to use for accessing the Credentials service.
     * @param pwdEncoder 用于验证散列（哈希）密码的对象。
     *                  The object to use for validating hashed passwords.
     * @param tracer The tracer instance.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    @Autowired
    public UsernamePasswordAuthProvider(
            final CredentialsClient credentialsClient,
            final HonoPasswordEncoder pwdEncoder,
            final Tracer tracer) {

        super(credentialsClient, tracer);
        this.pwdEncoder = Objects.requireNonNull(pwdEncoder);
    }

    /**
     * 根据设备提供的身份验证信息创建 {@link UsernamePasswordCredentials} 实例。
     * Creates a {@link UsernamePasswordCredentials} instance from auth info provided by a
     * device.
     * <p>
     *     传入的 JSON 对象需要包含 <em>username</em> 和 <em>password</em> 属性。
     * The JSON object passed in is required to contain a <em>username</em> and a
     * <em>password</em> property.
     *
     * @param authInfo 设备提供的凭据。 这些通常通过 {@link AuthHandler#parseCredentials(org.eclipse.hono.util.ExecutionContext)} 组装。
     *              The credentials provided by the device. These usually get assembled via
     *            {@link AuthHandler#parseCredentials(org.eclipse.hono.util.ExecutionContext)}.
     * @return 从身份验证信息创建的 {@link UsernamePasswordCredentials} 实例，如果身份验证信息不包含所需信息，则为 {@code null}。
     *          The {@link UsernamePasswordCredentials} instance created from the auth info or
     *         {@code null} if the auth info does not contain the required information.
     * @throws NullPointerException if the auth info is {@code null}.
     */
    @Override
    public UsernamePasswordCredentials getCredentials(final JsonObject authInfo) {

        final String username = JsonHelper.getValue(authInfo, CredentialsConstants.FIELD_USERNAME, String.class, null);
        final String password = JsonHelper.getValue(authInfo, CredentialsConstants.FIELD_PASSWORD, String.class, null);
        if (username == null || password == null) {
            return null;
        }
        final JsonObject clientContext = authInfo.copy();
        clientContext.remove(CredentialsConstants.FIELD_USERNAME);
        clientContext.remove(CredentialsConstants.FIELD_PASSWORD);

        if (password.isEmpty()) {
            //如果密码为空字符串，则从用户名中解析用户名:密码
            return tryGetCredentialsEncodedInUsername(username, clientContext);
        } else {
            return UsernamePasswordCredentials.create(username, password, clientContext);
        }
    }

    private UsernamePasswordCredentials tryGetCredentialsEncodedInUsername(
            final String username,
            final JsonObject clientContext) {

        try {
            final String decoded = new String(Base64.getDecoder().decode(username));
            final int colonIdx = decoded.indexOf(":");
            if (colonIdx > -1) {
                final String user = decoded.substring(0, colonIdx);
                final String pass = decoded.substring(colonIdx + 1);
                return UsernamePasswordCredentials.create(user, pass, clientContext);
            } else {
                return null;
            }
        } catch (final IllegalArgumentException ex) {
            log.debug("error extracting username/password from username field", ex);
            return null;
        }
    }

    @Override
    protected Future<Device> doValidateCredentials(
            final UsernamePasswordCredentials deviceCredentials,
            final CredentialsObject credentialsOnRecord) {

        final Context currentContext = Vertx.currentContext();
        if (currentContext == null) {
            return Future.failedFuture(new IllegalStateException("not running on vert.x Context"));
        } else {
            final Promise<Device> result = Promise.promise();
            currentContext.executeBlocking(blockingCodeHandler -> {
                log.debug("validating password hash on vert.x worker thread [{}]", Thread.currentThread().getName());
                final boolean isValid = credentialsOnRecord.getCandidateSecrets().stream()
                        .anyMatch(candidateSecret -> pwdEncoder.matches(deviceCredentials.getPassword(), candidateSecret));
                if (isValid) {
                    blockingCodeHandler.complete(new Device(deviceCredentials.getTenantId(), credentialsOnRecord.getDeviceId()));
                } else {
                    blockingCodeHandler.fail(new ClientErrorException(HttpURLConnection.HTTP_UNAUTHORIZED, "bad credentials"));
                }
            }, false, result);
            return result.future();
        }
    }
}
