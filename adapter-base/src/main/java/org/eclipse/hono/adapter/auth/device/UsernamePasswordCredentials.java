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

import java.util.Objects;

import org.eclipse.hono.util.CredentialsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

/**
 * 帮助类将身份验证期间设备提供的用户名/密码凭据解析为与 Hono 的 <em>Credentials</em> API 一起使用的属性。
 * Helper class to parse username/password credentials provided by devices during authentication into properties to be
 * used with Hono's <em>Credentials</em> API.
 * <p>
 *     属性确定如下：
 * The properties are determined as follows:
 * <ul>
 * <li>
 *     <em>password</em> 始终设置为给定的密码。
 *     <em>password</em> is always set to the given password.
 * </li>
 * <li>
 *     给定的用户名在 <code>&#64;('@'的html编码)</code> 符号的第一次出现周围被分成两部分。 <em>authId</em> 然后设置为第一部分，<em>tenantId</em> 设置为第二部分。
 *     the given username is split in two around the first occurrence of the <code>&#64;</code> sign. <em>authId</em> is
 * then set to the first part and <em>tenantId</em> is set to the second part.</li>
 * </ul>
 */
public class UsernamePasswordCredentials extends AbstractDeviceCredentials {

    private static final Logger LOG  = LoggerFactory.getLogger(UsernamePasswordCredentials.class);

    private String password;

    private UsernamePasswordCredentials(final String tenantId, final String authId, final JsonObject clientContext) {
        super(tenantId, authId, clientContext);
    }

    /**
     * Creates a new instance for a set of credentials.
     *
     * @param username The username provided by the device.
     * @param password The password provided by the device.
     * @return The credentials or {@code null} if the username does not contain a tenant ID.
     * @throws NullPointerException if any of the parameters is {@code null}.
     */
    public static final UsernamePasswordCredentials create(final String username, final String password) {
        return create(username, password, new JsonObject());
    }

    /**
     * 为一组凭据创建一个新实例。
     * Creates a new instance for a set of credentials.
     *
     * @param username The username provided by the device.
     * @param password The password provided by the device.
     * @param clientContext 可用于从 Credentials API 获取凭据的客户端上下文。
     *                      The client context that can be used to get credentials from the Credentials API.
     * @return The credentials or {@code null} if the username does not contain a tenant ID.
     * @throws NullPointerException if any of the parameters is {@code null}.
     */
    public static final UsernamePasswordCredentials create(
            final String username,
            final String password,
            final JsonObject clientContext) {

        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        Objects.requireNonNull(clientContext);

        // username consists of <userId>@<tenantId>
        final String[] userComponents = username.split("@", 2);
        if (userComponents.length != 2) {
            LOG.trace("username [{}] does not comply with expected pattern [<authId>@<tenantId>]", username);
            return null;
        }
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userComponents[1],
                userComponents[0], clientContext);
        credentials.password = password;
        return credentials;
    }

    /**
     * {@inheritDoc}
     *
     * @return Always {@link CredentialsConstants#SECRETS_TYPE_HASHED_PASSWORD}
     */
    @Override
    public final String getType() {
        return CredentialsConstants.SECRETS_TYPE_HASHED_PASSWORD;
    }

    /**
     * Gets the password to use for verifying the identity.
     *
     * @return The password.
     */
    public final String getPassword() {
        return password;
    }
}
