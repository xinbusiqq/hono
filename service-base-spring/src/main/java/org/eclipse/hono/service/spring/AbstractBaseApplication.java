/*******************************************************************************
 * Copyright (c) 2016, 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.hono.service.spring;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PreDestroy;

import org.eclipse.hono.config.ApplicationConfigProperties;
import org.eclipse.hono.service.HealthCheckProvider;
import org.eclipse.hono.service.HealthCheckServer;
import org.eclipse.hono.service.NoopHealthCheckServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.impl.JsonUtil;

/**
 * A base class for implementing Spring Boot applications.
 * <p>
 *     此类提供用于处理配置、Vert.x verticals和健康检查服务器的基本抽象。
 * This class provides basic abstractions for dealing with configuration, Vert.x verticals and health check server.
 */
public abstract class AbstractBaseApplication implements ApplicationRunner {

    /**
     * A logger to be shared with subclasses.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationConfigProperties config = new ApplicationConfigProperties();
    private Vertx vertx;

    private HealthCheckServer healthCheckServer = new NoopHealthCheckServer();

    /**
     * 设置要部署服务的 Vert.x 实例。
     * Sets the Vert.x instance to deploy the service to.
     *
     * @param vertx The vertx instance.
     * @throws NullPointerException if vertx is {@code null}.
     */
    @Autowired
    public final void setVertx(final Vertx vertx) {
        this.vertx = Objects.requireNonNull(vertx);
    }

    /**
     * 获取部署服务的 Vert.x 实例。
     * Gets the Vert.x instance the service gets deployed to.
     *
     * @return The vertx instance.
     */
    protected final Vertx getVertx() {
        return vertx;
    }

    /**
     * 设置用于此服务的应用程序配置属性。
     * Sets the application configuration properties to use for this service.
     *
     * @param config The properties.
     * @throws NullPointerException if the properties are {@code null}.
     */
    @Autowired(required = false)
    public final void setApplicationConfiguration(final ApplicationConfigProperties config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * 获取用于此服务的应用程序配置属性。
     * Gets the application configuration properties used for this service.
     *
     * @return The properties.
     */
    protected final ApplicationConfigProperties getConfig() {
        return config;
    }

    /**
     * 设置此应用程序的健康检查服务。
     * Sets the health check server for this application.
     *
     * @param healthCheckServer The health check server.
     * @throws NullPointerException if healthCheckServer is {@code null}.
     */
    @Autowired(required = false)
    public final void setHealthCheckServer(final HealthCheckServer healthCheckServer) {
        this.healthCheckServer = Objects.requireNonNull(healthCheckServer);
    }

    /**
     * 在部署服务之前，允许应用程序进行“飞行前”检查。
     * Allow the application to do a "pre-flight" check, before services are deployed.
     * <p>
     *     尽管当前的实现是空的，覆盖这个方法的类必须调用 super 方法，因为未来的实现可能会有所不同。
     * Although the current implementation is empty, classes overriding this method must call the super method, as
     * future implementations may be different.
     *
     * @throws IllegalStateException May be thrown if the implementor considers the application in state that it cannot
     *             be started up.
     */
    protected void preFlightCheck() throws IllegalStateException {
    }

    /**
     * 启动此应用程序。
     * Starts up this application.
     * <p>
     *     启动过程需要以下步骤：
     * The start up process entails the following steps:
     * <ol>
     * <li>调用 {@link #deployVerticles()} 来部署应该属于这个应用程序的所有 Verticle
     *  invoke {@link #deployVerticles()} to deploy all verticles that should be part of this application</li>
     * <li>调用 {@link #postDeployVerticles()} 以执行部署完成后的步骤
     *  invoke {@link #postDeployVerticles()} to perform any additional post deployment steps</li>
     * <li>启动健康检查服务
     *  start the health check server</li>
     * </ol>
     *
     * @param args The command line arguments provided to the application.
     */
    @Override
    public void run(final ApplicationArguments args) {

        if (vertx == null) {
            throw new IllegalStateException("no Vert.x instance has been configured");
        }

        preFlightCheck();

        if (log.isInfoEnabled()) {

            final String base64Encoder = Base64.getEncoder() == JsonUtil.BASE64_ENCODER ? "legacy" : "URL safe";

            log.info("running on Java VM [version: {}, name: {}, vendor: {}, max memory: {}MB, processors: {}] with vert.x using {} Base64 encoder",
                    System.getProperty("java.version"),
                    System.getProperty("java.vm.name"),
                    System.getProperty("java.vm.vendor"),
                    Runtime.getRuntime().maxMemory() >> 20,
                    CpuCoreSensor.availableProcessors(),
                    base64Encoder);
        }

        final CompletableFuture<Void> started = new CompletableFuture<>();
        log.info("开始加载Verticle，step1");
        deployVerticles()
            .compose(s -> postDeployVerticles())
            .compose(s -> healthCheckServer.start())
            .onSuccess(started::complete)
            .onFailure(started::completeExceptionally);

        started.join();
    }

    /**
     * 部署此应用程序所需的 Verticle。
     * Deploy verticles required by this application.
     * <p>
     *     这是一个通用的方法，部署所有应该属于这个应用程序的 Verticle。
     * This is a generic method, deploying all verticles that should be part of this application.
     * <p>
     *     当前的实现只返回一个成功的Future
     * Although the current implementation only returns a succeeded future, overriding this method it is required to
     * call "super", in order to enable future changes.
     *
     * @return A future indicating success. Application start-up fails if the returned future fails.
     */
    protected Future<Void> deployVerticles() {
        return Future.succeededFuture();
    }

    /**
     * 在成功部署应用程序 Verticle 后调用。
     * Invoked after the application verticles have been deployed successfully.
     * <p>
     *     可以被覆盖以提供额外的启动逻辑。
     * May be overridden to provide additional startup logic.
     * <p>
     *     这个默认实现只是返回一个成功的Future。
     * This default implementation simply returns a succeeded future.
     *
     * @return A future indicating success. Application start-up fails if the returned future fails.
     */
    protected Future<Void> postDeployVerticles() {
        return Future.succeededFuture();
    }

    /**
     * 以受控方式停止此应用程序。
     * Stops this application in a controlled fashion.
     */
    @PreDestroy
    public final void shutdown() {

        log.info("shutting down application...");
        final CompletableFuture<Void> shutdown = new CompletableFuture<>();

        preShutdown();
        healthCheckServer.stop()
            .onComplete(ok -> {
                vertx.close(attempt -> {
                    if (attempt.succeeded()) {
                        shutdown.complete(null);
                    } else {
                        shutdown.completeExceptionally(attempt.cause());
                    }
                });
            });
        shutdown.join();
    }

    /**
     * Invoked before application shutdown is initiated.
     * <p>
     * May be overridden to provide additional shutdown handling, e.g. releasing resources before the vert.x instance is
     * closed.
     */
    protected void preShutdown() {
        // empty
    }

    /**
     * 注册额外的健康检查。
     * Registers additional health checks.
     *
     * @param provider The provider of the health checks.
     */
    protected final void registerHealthchecks(final HealthCheckProvider provider) {
        Optional.ofNullable(provider).ifPresent(p -> {
            log.debug("registering health checks [provider: {}]", p.getClass().getName());
            healthCheckServer.registerHealthCheckResources(p);
        });
    }
}
