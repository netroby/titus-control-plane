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

package io.netflix.titus.api.loadbalancer.model.sanitizer;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.netflix.titus.api.connector.cloud.LoadBalancerConnector;
import io.netflix.titus.api.jobmanager.model.job.Container;
import io.netflix.titus.api.jobmanager.model.job.ContainerResources;
import io.netflix.titus.api.jobmanager.model.job.Image;
import io.netflix.titus.api.jobmanager.model.job.Job;
import io.netflix.titus.api.jobmanager.model.job.JobDescriptor;
import io.netflix.titus.api.jobmanager.model.job.JobState;
import io.netflix.titus.api.jobmanager.model.job.JobStatus;
import io.netflix.titus.api.jobmanager.model.job.ext.BatchJobExt;
import io.netflix.titus.api.jobmanager.model.job.ext.ServiceJobExt;
import io.netflix.titus.api.jobmanager.service.V3JobOperations;
import io.netflix.titus.api.loadbalancer.model.JobLoadBalancer;
import io.netflix.titus.api.loadbalancer.store.LoadBalancerStore;
import io.netflix.titus.runtime.store.v3.memory.InMemoryLoadBalancerStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultLoadBalancerValidatorTest {
    private static Logger logger = LoggerFactory.getLogger(DefaultLoadBalancerValidatorTest.class);

    private static final long TIMEOUT_MS = 30_000;

    private V3JobOperations jobOperations;
    private LoadBalancerStore loadBalancerStore;
    private LoadBalancerValidationConfiguration loadBalancerValidationConfiguration;
    private DefaultLoadBalancerValidator loadBalancerValidator;

    private static final String JOB_ID = "Titus-123";

    @Before
    public void setUp() throws Exception {
        LoadBalancerConnector client = mock(LoadBalancerConnector.class);
        jobOperations = mock(V3JobOperations.class);
        loadBalancerStore = new InMemoryLoadBalancerStore();
        loadBalancerValidationConfiguration = mock(LoadBalancerValidationConfiguration.class);

        loadBalancerValidator = DefaultLoadBalancerValidator.newBuilder()
                .withV3JobOperations(jobOperations)
                .withLoadBalancerClient(client)
                .withLoadBalancerStore(loadBalancerStore)
                .withLoadBalancerConfiguration(loadBalancerValidationConfiguration)
                .build();
    }

    @Test
    public void testValidateJobExists() throws Exception {
        when(jobOperations.getJob(JOB_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> loadBalancerValidator.validateJobId(JOB_ID)).hasMessageContaining("does not exist");
    }

    @Test
    public void testValidateJobAccepted() throws Exception {
        when(jobOperations.getJob(JOB_ID)).thenReturn(Optional.of(Job.newBuilder()
                .withId(JOB_ID)
                .withStatus(JobStatus.newBuilder()
                        .withState(JobState.Finished)
                        .build())
                .build()));
        assertThatThrownBy(() -> loadBalancerValidator.validateJobId(JOB_ID)).hasMessageContaining("is in state");
    }

    @Test
    public void testValidateJobIsService() throws Exception {
        when(jobOperations.getJob(JOB_ID)).thenReturn(Optional.of(Job.<BatchJobExt>newBuilder()
                .withId(JOB_ID)
                .withStatus(JobStatus.newBuilder()
                        .withState(JobState.Accepted)
                        .build())
                .withJobDescriptor(JobDescriptor.<BatchJobExt>newBuilder()
                        .build())
                .build()));
        assertThatThrownBy(() -> loadBalancerValidator.validateJobId(JOB_ID)).hasMessageContaining("is NOT of type service");
    }

    @Test
    public void testValidateJobAllocateIp() throws Exception {
        when(jobOperations.getJob(JOB_ID)).thenReturn(Optional.of(Job.<ServiceJobExt>newBuilder()
                .withId(JOB_ID)
                .withStatus(JobStatus.newBuilder()
                        .withState(JobState.Accepted)
                        .build())
                .withJobDescriptor(JobDescriptor.<ServiceJobExt>newBuilder()
                        .withExtensions(ServiceJobExt.newBuilder().build())
                        .withContainer(Container.newBuilder()
                                .withImage(Image.newBuilder().build())
                                .withContainerResources(ContainerResources.newBuilder()
                                        .withAllocateIP(false)
                                        .build())
                                .build())

                        .build())
                .build()));
        assertThatThrownBy(() -> loadBalancerValidator.validateJobId(JOB_ID)).hasMessageContaining("Job must request a routable IP");
    }

    @Test
    public void testValidateMaxLoadBalancers() throws Exception {
        when(jobOperations.getJob(JOB_ID)).thenReturn(Optional.of(Job.<ServiceJobExt>newBuilder()
                .withId(JOB_ID)
                .withStatus(JobStatus.newBuilder()
                        .withState(JobState.Accepted)
                        .build())
                .withJobDescriptor(JobDescriptor.<ServiceJobExt>newBuilder()
                        .withExtensions(ServiceJobExt.newBuilder().build())
                        .withContainer(Container.newBuilder()
                                .withImage(Image.newBuilder().build())
                                .withContainerResources(ContainerResources.newBuilder()
                                        .withAllocateIP(true)
                                        .build())
                                .build())
                        .build())
                .build()));

        when(loadBalancerValidationConfiguration.getMaxLoadBalancersPerJob()).thenReturn(30);

        for (int i = 0; i < loadBalancerValidationConfiguration.getMaxLoadBalancersPerJob() + 1; i++) {
            loadBalancerStore.addOrUpdateLoadBalancer(new JobLoadBalancer(JOB_ID, "LoadBalancer-" + i), JobLoadBalancer.State.Associated).await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        assertThatThrownBy(() -> loadBalancerValidator.validateJobId(JOB_ID)).hasMessageContaining("Number of load balancers for Job");
    }

    @Test
    public void testValidJob() throws Exception {
        when(jobOperations.getJob(JOB_ID)).thenReturn(Optional.of(Job.<ServiceJobExt>newBuilder()
                .withId(JOB_ID)
                .withStatus(JobStatus.newBuilder()
                        .withState(JobState.Accepted)
                        .build())
                .withJobDescriptor(JobDescriptor.<ServiceJobExt>newBuilder()
                        .withExtensions(ServiceJobExt.newBuilder().build())
                        .withContainer(Container.newBuilder()
                                .withImage(Image.newBuilder().build())
                                .withContainerResources(ContainerResources.newBuilder()
                                        .withAllocateIP(true)
                                        .build())
                                .build())
                        .build())
                .build()));
        assertThatCode(() -> loadBalancerValidator.validateJobId(JOB_ID)).doesNotThrowAnyException();
    }
}
