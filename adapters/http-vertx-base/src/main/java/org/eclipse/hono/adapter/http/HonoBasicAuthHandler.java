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

package org.eclipse.hono.adapter.http;

import java.util.Base64;
import java.util.Objects;

import org.eclipse.hono.adapter.auth.device.DeviceCredentialsAuthProvider;
import org.eclipse.hono.adapter.auth.device.ExecutionContextAuthHandler;
import org.eclipse.hono.adapter.auth.device.PreCredentialsValidationHandler;
import org.eclipse.hono.service.http.HttpContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler;


/**
 * vert.x web 的标准 {@code BasicAuthHandlerImpl} 的 Hono 特定版本，
 * 当发生身份验证失败时，它提取和处理 {@link org.eclipse.hono.client.ServiceInvocationException}
 * 作为 {@code HttpStatusException} 中的根本原因 .
 * A Hono specific version of vert.x web's standard {@code BasicAuthHandlerImpl}
 * that extracts and handles a {@link org.eclipse.hono.client.ServiceInvocationException} conveyed as the
 * root cause in an {@code HttpStatusException} when an authentication failure
 * occurs.
 * <p>
 * 除此之外，这里还添加了对 {@link PreCredentialsValidationHandler} 和将 span 上下文传输到 AuthProvider 的支持。
 * Apart from that, support for a {@link PreCredentialsValidationHandler} and for
 * transferring a span context to the AuthProvider is added here.
 *
 */
public class HonoBasicAuthHandler extends HTTPAuthorizationHandler<AuthenticationProvider> implements BasicAuthHandler {

    private final PreCredentialsValidationHandler<HttpContext> preCredentialsValidationHandler;

    /**
     * Creates a new handler for an authentication provider and a realm name.
     *
     * @param authProvider The provider to use for validating credentials.
     * @param realm The realm name.
     * @throws NullPointerException If authProvider is {@code null}.
     */
    public HonoBasicAuthHandler(final DeviceCredentialsAuthProvider<?> authProvider, final String realm) {
        this(authProvider, realm, (PreCredentialsValidationHandler<HttpContext>) null);
    }

    /**
     * 为身份验证提供者和领域名称创建一个新的处理程序。
     * Creates a new handler for an authentication provider and a realm name.
     *
     * @param authProvider 用于验证凭据的提供程序。
     *                     The provider to use for validating credentials.
     * @param realm 领域名称
     *              The realm name.
     * @param preCredentialsValidationHandler 在确定凭据之后和验证之前调用的可选处理程序。
     *            可用于在完成可能昂贵的凭据验证之前使用凭据和租户信息执行检查。
     *            处理程序返回的失败future将使相应的身份验证尝试失败。
     *            An optional handler to invoke after the credentials got determined and
     *            before they get validated. Can be used to perform checks using the credentials and tenant information
     *            before the potentially expensive credentials validation is done. A failed future returned by the
     *            handler will fail the corresponding authentication attempt.
     * @throws NullPointerException If authProvider is {@code null}.
     */
    public HonoBasicAuthHandler(
            final DeviceCredentialsAuthProvider<?> authProvider,
            final String realm,
            final PreCredentialsValidationHandler<HttpContext> preCredentialsValidationHandler) {

        super(Objects.requireNonNull(authProvider), Type.BASIC, realm);
        this.preCredentialsValidationHandler = preCredentialsValidationHandler;
    }

    @Override
    public void authenticate(final RoutingContext context, final Handler<AsyncResult<User>> handler) {

        parseAuthorization(context, parseAuthorization -> {
            if (parseAuthorization.failed()) {
                handler.handle(Future.failedFuture(parseAuthorization.cause()));
                return;
            }

            final String suser;
            final String spass;

            try {
                // decode the payload
                final String decoded = new String(Base64.getDecoder().decode(parseAuthorization.result()));

                final int colonIdx = decoded.indexOf(":");
                if (colonIdx != -1) {
                    suser = decoded.substring(0, colonIdx);
                    spass = decoded.substring(colonIdx + 1);
                } else {
                    suser = decoded;
                    spass = null;
                }
            } catch (RuntimeException e) {
                handler.handle(Future.failedFuture(new HttpException(400, e)));
                return;
            }

            final var credentials = new JsonObject()
                    .put("username", suser)
                    .put("password", spass);

            final ExecutionContextAuthHandler<HttpContext> authHandler = new ExecutionContextAuthHandler<>(
                    (DeviceCredentialsAuthProvider<?>) authProvider,
                    preCredentialsValidationHandler) {

                @Override
                public Future<JsonObject> parseCredentials(final HttpContext context) {
                    return Future.succeededFuture(credentials);
                }
            };

            authHandler.authenticateDevice(HttpContext.from(context))
                .map(deviceUser -> (User) deviceUser)
                .onComplete(handler);
        });

    }

    /**
     * Fails the context with the error code determined from an exception.
     * <p>
     * This method invokes {@link AuthHandlerTools#processException(RoutingContext, Throwable, String)}.
     *
     * @param ctx The routing context.
     * @param exception The cause of failure to process the request.
     */
    @Override
    protected void processException(final RoutingContext ctx, final Throwable exception) {

        if (ctx.response().ended()) {
            return;
        }

        AuthHandlerTools.processException(ctx, exception, authenticateHeader(ctx));
    }
}
