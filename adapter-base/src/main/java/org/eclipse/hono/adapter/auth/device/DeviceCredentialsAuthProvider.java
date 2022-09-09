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

import org.eclipse.hono.service.auth.DeviceUser;

import io.opentracing.SpanContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.AuthenticationProvider;

/**
 * 用于验证凭据的身份验证提供程序。
 * An authentication provider for verifying credentials.
 *
 * @param <T> The type of credentials that this provider can validate.
 */
public interface DeviceCredentialsAuthProvider<T extends AbstractDeviceCredentials> extends AuthenticationProvider {

    /**
     * 根据设备记录的凭据验证设备提供的凭据。
     * Validates credentials provided by a device against the credentials on record
     * for the device.
     * <p>
     *     使用 Hono 的 <a href="https://www.eclipse.org/hono/docs/api/credentials/">Credentials API</a> 检索记录在案的凭据。
     * The credentials on record are retrieved using Hono's
     *  <a href="https://www.eclipse.org/hono/docs/api/credentials/">Credentials API</a>.
     *
     * @param credentials 设备提供的凭据。
     *                    The credentials provided by the device.
     * @param spanContext The SpanContext (may be null).
     * @param resultHandler 通知验证结果的处理程序。 如果验证成功，则结果包含一个表示已验证设备的对象。
     *                      The handler to notify about the outcome of the validation. If validation succeeds,
     *                      the result contains an object representing the authenticated device.
     * @throws NullPointerException if any of the parameters is {@code null}.
     */
    void authenticate(T credentials, SpanContext spanContext, Handler<AsyncResult<DeviceUser>> resultHandler);

    /**
     * 根据设备提供的身份验证信息创建设备凭据。
     * Creates device credentials from authentication information provided by a
     * device.
     * <p>
     *     子类需要根据 JSON 对象中包含的信息创建具体的 {@code DeviceCredentials} 实例。
     * Subclasses need to create a concrete {@code DeviceCredentials} instance based on
     * the information contained in the JSON object.
     *
     * @param authInfo 设备提供的凭据。这些通常通过 {@link AuthHandler#parseCredentials(org.eclipse.hono.util.ExecutionContext)} 组装。
     *              The credentials provided by the device. These usually get assembled via
     *            {@link AuthHandler#parseCredentials(org.eclipse.hono.util.ExecutionContext)}.
     * @return 返回设备的认证信息，或null（如果身份认证信息不包含必需的属性）
     *         The device credentials or {@code null} if the auth info does not contain the required information.
     * @throws NullPointerException if auth info is {@code null}.
     */
    T getCredentials(JsonObject authInfo);
}
