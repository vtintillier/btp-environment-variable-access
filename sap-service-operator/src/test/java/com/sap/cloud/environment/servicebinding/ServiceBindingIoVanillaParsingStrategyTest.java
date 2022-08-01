/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBindingIoVanillaParsingStrategyTest
{
    @Test
    void testParseDataBinding()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoVanillaParsingStrategyTest.class, "data-xsuaa-binding");

        final ServiceBindingIoVanillaParsingStrategy sut = ServiceBindingIoVanillaParsingStrategy.newDefault();

        final ServiceBinding serviceBinding = sut.parse("data-xsuaa-binding", rootDirectory).orElse(null);

        assertThat(serviceBinding).isNotNull();

        // without proper metadata we cannot correctly distinguish between credentials and actual meta properties of the binding
        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "credentials");

        assertThat(serviceBinding.getName().orElse(null)).isEqualTo("data-xsuaa-binding");
        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getServicePlan()).isEmpty();
        assertThat(serviceBinding.getTags()).isEmpty();

        assertThat(serviceBinding.getCredentials().keySet())
            .containsExactlyInAnyOrder(
                "clientid",
                "clientsecret",
                "domains",
                "instance_guid",
                "instance_name",
                "plan",
                "tags");
        assertThat(serviceBinding.getCredentials().get("clientid")).isEqualTo("data-xsuaa-clientid");
        assertThat(serviceBinding.getCredentials().get("clientsecret")).isEqualTo("data-xsuaa-clientsecret");
        assertThat(serviceBinding.getCredentials().get("domains"))
            .asList()
            .containsExactly("data-xsuaa-domain-1", "data-xsuaa-domain-2");
        assertThat(serviceBinding.getCredentials().get("instance_guid")).isEqualTo("data-xsuaa-instance-guid");
        assertThat(serviceBinding.getCredentials().get("instance_name")).isEqualTo("data-xsuaa-instance-name");
        assertThat(serviceBinding.getCredentials().get("plan")).isEqualTo("application");
        assertThat(serviceBinding.getCredentials().get("tags"))
            .asList()
            .containsExactly("data-xsuaa-tag-1", "data-xsuaa-tag-2");
    }

    @Test
    void testParseServicebindingIoBinding()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoVanillaParsingStrategyTest.class, "servicebinding-io-binding");

        final ServiceBindingIoVanillaParsingStrategy sut = ServiceBindingIoVanillaParsingStrategy.newDefault();

        final ServiceBinding serviceBinding = sut.parse("binding", rootDirectory).orElse(null);

        assertThat(serviceBinding).isNotNull();

        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "provider", "credentials");

        assertThat(serviceBinding.getName().orElse(null)).isEqualTo("binding");
        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getServicePlan()).isEmpty();
        assertThat(serviceBinding.getTags()).isEmpty();

        assertThat(serviceBinding.getCredentials().keySet())
            .containsExactlyInAnyOrder("certificates", "host", "password", "port", "private-key", "uri", "username");
        assertThat(serviceBinding.getCredentials().get("certificates"))
            .asList()
            .containsExactly("some-certificate-1", "some-certificate-2");
        assertThat(serviceBinding.getCredentials().get("host")).isEqualTo("https://some.host");
        assertThat(serviceBinding.getCredentials().get("password")).isEqualTo("some-password");
        assertThat(serviceBinding.getCredentials().get("port")).isEqualTo(443);
        assertThat(serviceBinding.getCredentials().get("private-key")).isEqualTo("some-private-key");
        assertThat(serviceBinding.getCredentials().get("uri")).isEqualTo("https://some.host:443/endpoint");
        assertThat(serviceBinding.getCredentials().get("username")).isEqualTo("some-username");
    }

    @Test
    void parseIgnoresMetadataBinding()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoVanillaParsingStrategyTest.class, "metadata-binding");

        final ServiceBindingIoVanillaParsingStrategy sut = ServiceBindingIoVanillaParsingStrategy.newDefault();

        final Optional<ServiceBinding> maybeServiceBinding = sut.parse("binding", rootDirectory);

        assertThat(maybeServiceBinding).isEmpty();
    }
}
