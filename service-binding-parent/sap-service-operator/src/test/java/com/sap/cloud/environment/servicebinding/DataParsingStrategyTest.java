/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.sap.cloud.environment.api.ServiceBinding;

import static org.assertj.core.api.Assertions.assertThat;

class DataParsingStrategyTest
{
    @Test
    @SuppressWarnings( "unchecked" )
    void parseValidBinding() throws IOException
    {
        final Path path = TestResource.get(DataParsingStrategyTest.class, "ValidBinding");

        final DataParsingStrategy sut = DataParsingStrategy.newDefault();

        final ServiceBinding serviceBinding = sut.parse("XSUAA", "my-xsuaa-binding", path).orElse(null);

        assertThat(serviceBinding).isNotNull();

        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("instance_guid",
                                                                       "instance_name",
                                                                       "label",
                                                                       "plan",
                                                                       "tags",
                                                                       "credentials");

        assertThat(serviceBinding.getName().orElse("")).isEqualTo("my-xsuaa-binding");
        assertThat(serviceBinding.getServiceName().orElse("")).isEqualTo("XSUAA");
        assertThat(serviceBinding.getServicePlan().orElse("")).isEqualTo("my-plan");
        assertThat(serviceBinding.getTags()).containsExactly("my-tag-1", "my-tag-2");

        assertThat(serviceBinding.getCredentials()).containsOnlyKeys("domains", "clientid", "clientsecret");
        assertThat((List<Object>) serviceBinding.getCredentials().get("domains")).containsExactly("my-domain-1",
                                                                                                  "my-domain-2");
        assertThat(serviceBinding.getCredentials()).containsEntry("clientid", "my-clientid");
        assertThat(serviceBinding.getCredentials()).containsEntry("clientsecret", "my-clientsecret");
    }

    @Test
    void parseWithoutCredentialsLeadsToEmptyResult() throws IOException
    {
        final Path path = TestResource.get(DataParsingStrategyTest.class, "NoCredentials");

        final DataParsingStrategy sut = DataParsingStrategy.newDefault();

        final Optional<ServiceBinding> serviceBinding;
        serviceBinding = sut.parse("XSUAA", "my-xsuaa-binding", path);

        assertThat(serviceBinding.isPresent()).isFalse();
    }
}