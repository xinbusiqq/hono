/*******************************************************************************
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
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

import io.vertx.core.json.JsonObject;

/**
 * 由设备提供的用于身份验证的凭据的包装器。
 * A wrapper around credentials provided by a device for authentication.
 *
 */
public interface DeviceCredentials {

    /**
     * 获取此实例表示的凭据类型。
     * Gets the type of credentials this instance represents.
     *
     * @return The type.
     */
    String getType();

    /**
     * 获取要进行身份验证的设备标识。
     * Gets the identity that the device wants to authenticate as.
     *
     * @return The identity.
     */
    String getAuthId();

    /**
     * 获取设备声明所属的租户。
     * Gets the tenant that the device claims to belong to.
     *
     * @return The tenant.
     */
    String getTenantId();

    /**
     * 获取设备的其他属性。
     * Gets additional properties of the device.
     * <p>
     *     实现可以返回包含任意属性的 JSON 对象。
     * An implementation can return a JSON object containing arbitrary properties.
     * <p>
     *     默认实现返回一个新的空 JSON 对象。
     * The default implementation returns a new, empty JSON object.
     *
     * @return 表示客户端上下文的属性。The properties representing the client context.
     */
    default JsonObject getClientContext() {
        return new JsonObject();
    }
}
