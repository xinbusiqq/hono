/**
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */


package org.eclipse.hono.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

/**
 * A producer of {@link ApplicationConfigProperties}.
 *
 */
@ApplicationScoped
public class ApplicationPropertiesProducer {

    @Produces
    @Singleton
    ApplicationConfigProperties applicationProperties(final ApplicationOptions appOptions) {
        return new ApplicationConfigProperties(appOptions);
    }
}
