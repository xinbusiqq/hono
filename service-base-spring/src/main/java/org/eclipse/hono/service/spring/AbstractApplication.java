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

package org.eclipse.hono.service.spring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.hono.service.AbstractServiceBase;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/**
 * A base class for implementing Spring Boot applications.
 * <p>
 * This class requires that an instance of {@link ObjectFactory} is provided
 * ({@link #addServiceFactories(Set)} for each service to be exposed by this application.
 */
public class AbstractApplication extends AbstractBaseApplication {

    private final Set<ObjectFactory<? extends AbstractServiceBase<?>>> serviceFactories = new HashSet<>();

    @Override
    protected void preFlightCheck() throws IllegalStateException {
        if (serviceFactories.isEmpty()) {
            throw new IllegalStateException("no service factory has been configured");
        }
    }

    /**
     * 添加用于创建服务实例的工厂，以便在启动期间部署 Vert.x 容器。
     * Adds the factories to use for creating service instances to
     * deploy the Vert.x container during startup.
     * <p>
     *
     * @param factories The service factories.
     * @throws NullPointerException if factories is {@code null}.
     */
    @Autowired
    public final void addServiceFactories(final Set<ObjectFactory<? extends AbstractServiceBase<?>>> factories) {
        Objects.requireNonNull(factories);
        serviceFactories.addAll(factories);
        log.debug("added {} service factories", factories.size());
    }

    @Override
    protected Future<Void> deployVerticles() {
        // call into super ...
        return super.deployVerticles()
                // ... then deploy the required verticles
                .compose(s -> deployRequiredVerticles(getConfig().getMaxInstances()))
                // ... then deploy service verticles
                .compose(s -> deployServiceVerticles(getConfig().getMaxInstances()));
    }

    /**
     * 在部署服务实例之前调用。
     * Invoked before the service instances are being deployed.
     * <p>
     *     可能会被覆盖以准备服务实例的环境，例如 部署额外的（先决条件）verticles。
     * May be overridden to prepare the environment for the service instances, e.g. deploying additional (prerequisite)
     * verticles.
     * <p>
     *     这个默认实现只是返回一个成功的future。
     * This default implementation simply returns a succeeded future.
     *
     * @param maxInstances The number of service verticle instances to deploy.要部署的服务 Verticle 实例数。
     * @return A future indicating success. Application start-up fails if the returned future fails.
     * 预示成功的Future。 如果返回的 future 失败，应用程序启动失败。
     */
    protected Future<Void> deployRequiredVerticles(final int maxInstances) {
        return Future.succeededFuture();
    }

    /**
     * 部署服务实例。
     * Deploys the service instances.
     *
     * @param maxInstances The number of instances to deploy.
     * @return A future indicating the outcome of the operation. The future will
     *         be succeeded if the service instances have been deployed successfully.
     *         指示操作结果的future。 如果服务实例部署成功，future 就会成功。
     */
    private Future<Void> deployServiceVerticles(final int maxInstances) {

        @SuppressWarnings("rawtypes")
        final List<Future> deploymentTracker = new ArrayList<>();

        for (final ObjectFactory<? extends AbstractServiceBase<?>> serviceFactory : serviceFactories) {

            for (int i = 1; i <= maxInstances; i++) {
                final Promise<String> deployPromise = Promise.promise();
                final AbstractServiceBase<?> serviceInstance = serviceFactory.getObject();
                preDeploy(serviceInstance);
                log.debug("deploying service instance #{} [type: {}]", i, serviceInstance.getClass().getName());
                getVertx().deployVerticle(serviceInstance, deployPromise);
                deploymentTracker.add(deployPromise.future().map(id -> {
                    postDeploy(serviceInstance);
                    return id;
                }));
            }
        }

        return CompositeFuture.all(deploymentTracker).mapEmpty();
    }

    /**
     * 在服务实例对象部署到 vert.x 之前调用。
     * Invoked before a service instance object gets deployed to vert.x.
     * <p>
     *     这个默认实现什么都不做。
     * This default implementation does nothing.
     *
     * @param serviceInstance The instance.
     */
    protected void preDeploy(final AbstractServiceBase<?> serviceInstance) {
        // do nothing
    }

    /**
     * 在服务实例对象成功部署到 vert.x 后调用。
     * Invoked after a service instance object has been deployed successfully to vert.x.
     * <p>
     *     这个默认实现什么都不做。
     * This default implementation does nothing.
     *
     * @param serviceInstance The instance.
     */
    protected void postDeploy(final AbstractServiceBase<?> serviceInstance) {
        // do nothing
    }

}
