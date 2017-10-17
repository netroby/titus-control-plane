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

package io.netflix.titus.api.jobmanager.model.job.sanitizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import io.netflix.titus.api.jobmanager.model.job.Container;
import io.netflix.titus.api.jobmanager.model.job.ContainerResources;
import io.netflix.titus.api.model.ResourceDimension;

/**
 */
public class JobAssertions {

    private static final Pattern SG_PATTERN = Pattern.compile("sg-.*");

    private final Function<String, ResourceDimension> maxContainerSizeResolver;

    public JobAssertions(Function<String, ResourceDimension> maxContainerSizeResolver) {
        this.maxContainerSizeResolver = maxContainerSizeResolver;
    }

    public boolean isValidSyntax(List<String> securityGroups) {
        return securityGroups.stream().allMatch(sg -> SG_PATTERN.matcher(sg).matches());
    }

    public Map<String, String> notExceedsComputeResources(String capacityGroup, Container container) {
        ResourceDimension maxContainerSize = maxContainerSizeResolver.apply(capacityGroup);
        ContainerResources resources = container.getContainerResources();

        Map<String, String> violations = new HashMap<>();
        check(resources::getCpu, maxContainerSize::getCpu).ifPresent(v -> violations.put("container.containerResources.cpu", v));
        check(resources::getGpu, maxContainerSize::getGpu).ifPresent(v -> violations.put("container.containerResources.gpu", v));
        check(resources::getMemoryMB, maxContainerSize::getMemoryMB).ifPresent(v -> violations.put("container.containerResources.memoryMB", v));
        check(resources::getDiskMB, maxContainerSize::getDiskMB).ifPresent(v -> violations.put("container.containerResources.diskMB", v));
        check(resources::getNetworkMbps, maxContainerSize::getNetworkMbs).ifPresent(v -> violations.put("container.containerResources.networkMbps", v));

        return violations;
    }

    private <N extends Number> Optional<String> check(Supplier<N> jobResource, Supplier<N> maxAllowed) {
        if (jobResource.get().doubleValue() > maxAllowed.get().doubleValue()) {
            return Optional.of("Above maximum allowed value " + maxAllowed.get());
        }
        return Optional.empty();
    }
}