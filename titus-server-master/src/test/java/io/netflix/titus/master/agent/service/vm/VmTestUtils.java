/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.netflix.titus.master.agent.service.vm;

import io.netflix.titus.master.agent.service.AgentManagementConfiguration;
import io.netflix.titus.testkit.rx.ExtTestSubscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Container for common functions and constantsł.
 */
public final class VmTestUtils {

    public static final long CACHE_REFRESH_INTERVAL_MS = 1_000;
    public static final long FULL_CACHE_REFRESH_INTERVAL_MS = 10_000;

    public static AgentManagementConfiguration mockedAgentManagementConfiguration() {
        AgentManagementConfiguration configuration = mock(AgentManagementConfiguration.class);
        when(configuration.getCacheRefreshIntervalMs()).thenReturn(CACHE_REFRESH_INTERVAL_MS);
        when(configuration.getFullCacheRefreshIntervalMs()).thenReturn(FULL_CACHE_REFRESH_INTERVAL_MS);
        when(configuration.getAgentServerGroupPattern()).thenReturn(".*");
        return configuration;
    }

    public static void expectServerGroupUpdateEvent(ExtTestSubscriber<CacheUpdateEvent> eventSubscriber, String serverGroupId) {
        CacheUpdateEvent event = eventSubscriber.takeNext();
        assertThat(event).isNotNull();
        assertThat(event.getType()).isEqualTo(CacheUpdateType.ServerGroup);
        assertThat(event.getResourceId()).isEqualTo(serverGroupId);
    }

    public static void expectServerUpdateEvent(ExtTestSubscriber<CacheUpdateEvent> eventSubscriber, String serverId) {
        CacheUpdateEvent event = eventSubscriber.takeNext();
        assertThat(event).isNotNull();
        assertThat(event.getType()).isEqualTo(CacheUpdateType.Server);
        assertThat(event.getResourceId()).isEqualTo(serverId);
    }
}