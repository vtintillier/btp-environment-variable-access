/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceBindingIoMetadataParsingStrategyTest
{
    @Test
    void parseValidDataBinding()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoMetadataParsingStrategyTest.class, "data-xsuaa-binding");

        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();

        final ServiceBinding serviceBinding = sut.parse("data-xsuaa-binding", rootDirectory).orElse(null);

        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getName().orElse(null)).isEqualTo("data-xsuaa-binding");
        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getServicePlan().orElse(null)).isEqualTo("application");
        assertThat(serviceBinding.getTags()).containsExactlyInAnyOrder("data-xsuaa-tag-1", "data-xsuaa-tag-2");
        assertThat(serviceBinding.get("instance_guid").orElse(null)).isEqualTo("data-xsuaa-instance-guid");
        assertThat(serviceBinding.get("instance_name").orElse(null)).isEqualTo("data-xsuaa-instance-name");
        assertThat(serviceBinding.getCredentials()).isNotEmpty();
        assertThat(serviceBinding.getCredentials().get("clientid")).isEqualTo("data-xsuaa-clientid");
        assertThat(serviceBinding.getCredentials().get("clientsecret")).isEqualTo("data-xsuaa-clientsecret");
        assertThat(serviceBinding.getCredentials().get("domains"))
            .asList()
            .containsExactlyInAnyOrder("data-xsuaa-domain-1", "data-xsuaa-domain-2");
    }

    @Test
    void parseValidSecretKeyBinding()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoMetadataParsingStrategyTest.class, "secret-key-xsuaa-binding");

        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();

        final ServiceBinding serviceBinding = sut.parse("secret-key-xsuaa-binding", rootDirectory).orElse(null);

        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getName().orElse(null)).isEqualTo("secret-key-xsuaa-binding");
        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getServicePlan().orElse(null)).isEqualTo("lite");
        assertThat(serviceBinding.getTags())
            .containsExactlyInAnyOrder("secret-key-xsuaa-tag-1", "secret-key-xsuaa-tag-2");
        assertThat(serviceBinding.get("instance_guid").orElse(null)).isEqualTo("secret-key-xsuaa-instance-guid");
        assertThat(serviceBinding.get("instance_name").orElse(null)).isEqualTo("secret-key-xsuaa-instance-name");
        assertThat(serviceBinding.getCredentials()).isNotEmpty();
        assertThat(serviceBinding.getCredentials().get("clientid")).isEqualTo("secret-key-xsuaa-clientid");
        assertThat(serviceBinding.getCredentials().get("clientsecret")).isEqualTo("secret-key-xsuaa-clientsecret");
        assertThat(serviceBinding.getCredentials().get("url")).isEqualTo("https://secret-key-xsuaa-domain-1.com");
        assertThat(serviceBinding.getCredentials().get("zone_uuid")).isEqualTo("secret-key-xsuaa-zone-uuid");
        assertThat(serviceBinding.getCredentials().get("domain")).isEqualTo("secret-key-xsuaa-domain-1");
        assertThat(serviceBinding.getCredentials().get("domains"))
            .asList()
            .containsExactlyInAnyOrder("secret-key-xsuaa-domain-1");
    }

    @Test
    void parseBindingWithoutMetadataLeadsToEmptyResult()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoMetadataParsingStrategyTest.class, "without-metadata");

        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();

        final Optional<ServiceBinding> maybeServiceBinding = sut.parse("foo", rootDirectory);

        assertThat(maybeServiceBinding).isEmpty();
    }

    @Test
    void parseBindingWithoutCredentialsLeadsToEmptyResult()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoMetadataParsingStrategyTest.class, "without-credentials");

        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();

        final Optional<ServiceBinding> maybeServiceBinding = sut.parse("binding", rootDirectory);

        assertThat(maybeServiceBinding).isEmpty();
    }

    @Test
    void parseBindingWithoutTypeProperty()
        throws IOException
    {
        final Path rootDirectory =
            TestResource.get(ServiceBindingIoMetadataParsingStrategyTest.class, "without-type-property");

        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();

        final ServiceBinding serviceBinding = sut.parse("binding", rootDirectory).orElse(null);

        assertThat(serviceBinding).isNotNull();

        assertThat(serviceBinding.getKeys())
            .containsExactlyInAnyOrder("tags", "plan", "instance_guid", "instance_name", "credentials");
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void parseIgnoresEmptyJsonProperty( @Nonnull @TempDir final Path rootDirectory )
        throws IOException
    {
        // setup file system
        final Path bindingRoot = rootDirectory.resolve("binding");
        write(
            bindingRoot.resolve(".metadata"),
            "{\n"
                + "    \"metaDataProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"type\",\n"
                + "            \"format\": \"text\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"empty_text\",\n"
                + "            \"format\": \"text\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"empty_json\",\n"
                + "            \"format\": \"json\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"empty_container\",\n"
                + "            \"format\": \"json\",\n"
                + "            \"container\": true\n"
                + "        }\n"
                + "    ],\n"
                + "    \"credentialProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"token\",\n"
                + "            \"format\": \"text\"\n"
                + "        }\n"
                + "    ]\n"
                + "}");
        write(bindingRoot.resolve("type"), "xsuaa");
        write(bindingRoot.resolve("empty_text"), "");
        write(bindingRoot.resolve("empty_json"), "");
        write(bindingRoot.resolve("empty_container"), "");
        write(bindingRoot.resolve("token"), "auth-token");

        // setup environment variable reader
        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        // setup subject under test
        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();
        final ServiceBinding serviceBinding = sut.parse("binding", bindingRoot).orElse(null);

        // assert
        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "empty_text", "credentials");

        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getCredentials().get("token")).isEqualTo("auth-token");
        assertThat(serviceBinding.get("empty_text").orElse(null)).isEqualTo("");
        assertThat(serviceBinding.get("empty_json")).isEmpty();
        assertThat(serviceBinding.get("empty_container")).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void parseIgnoresPropertyWithoutFile( @Nonnull @TempDir final Path rootDirectory )
        throws IOException
    {
        // setup file system
        final Path bindingRoot = rootDirectory.resolve("binding");
        write(
            bindingRoot.resolve(".metadata"),
            "{\n"
                + "    \"metaDataProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"type\",\n"
                + "            \"format\": \"text\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"missing_text\",\n"
                + "            \"format\": \"text\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"missing_json\",\n"
                + "            \"format\": \"json\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"missing_container\",\n"
                + "            \"format\": \"json\",\n"
                + "            \"container\": true\n"
                + "        }\n"
                + "    ],\n"
                + "    \"credentialProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"token\",\n"
                + "            \"format\": \"text\"\n"
                + "        }\n"
                + "    ]\n"
                + "}");
        write(bindingRoot.resolve("type"), "xsuaa");
        write(bindingRoot.resolve("token"), "auth-token");

        assertThat(Files.exists(bindingRoot.resolve("missing_text"))).isFalse();
        assertThat(Files.exists(bindingRoot.resolve("missing_json"))).isFalse();
        assertThat(Files.exists(bindingRoot.resolve("missing_container"))).isFalse();

        // setup environment variable reader
        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        // setup subject under test
        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();
        final ServiceBinding serviceBinding = sut.parse("binding", bindingRoot).orElse(null);

        // assert
        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "credentials");

        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getCredentials().get("token")).isEqualTo("auth-token");
        assertThat(serviceBinding.get("empty_text")).isEmpty();
        assertThat(serviceBinding.get("empty_json")).isEmpty();
        assertThat(serviceBinding.get("empty_container")).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void parseIgnoresPropertyWithoutMetadataEntry( @Nonnull @TempDir final Path rootDirectory )
        throws IOException
    {
        // setup file system
        final Path bindingRoot = rootDirectory.resolve("binding");
        write(
            bindingRoot.resolve(".metadata"),
            "{\n"
                + "    \"metaDataProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"type\",\n"
                + "            \"format\": \"text\"\n"
                + "        }\n"
                + "    ],\n"
                + "    \"credentialProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"token\",\n"
                + "            \"format\": \"text\"\n"
                + "        }\n"
                + "    ]\n"
                + "}");
        write(bindingRoot.resolve("type"), "xsuaa");
        write(bindingRoot.resolve("token"), "auth-token");
        write(bindingRoot.resolve("property"), "value");

        // setup environment variable reader
        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        // setup subject under test
        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();
        final ServiceBinding serviceBinding = sut.parse("binding", bindingRoot).orElse(null);

        // assert
        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "credentials");

        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getCredentials().get("token")).isEqualTo("auth-token");
        assertThat(serviceBinding.get("property")).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void parseIgnoresInvalidContainer( @Nonnull @TempDir final Path rootDirectory )
        throws IOException
    {
        // setup file system
        final Path bindingRoot = rootDirectory.resolve("binding");
        write(
            bindingRoot.resolve(".metadata"),
            "{\n"
                + "    \"metaDataProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"type\",\n"
                + "            \"format\": \"text\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"list_container\",\n"
                + "            \"format\": \"json\",\n"
                + "            \"container\": true\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"int_container\",\n"
                + "            \"format\": \"json\",\n"
                + "            \"container\": true\n"
                + "        }\n"
                + "    ],\n"
                + "    \"credentialProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"token\",\n"
                + "            \"format\": \"text\"\n"
                + "        }\n"
                + "    ]\n"
                + "}");
        write(bindingRoot.resolve("type"), "xsuaa");
        write(bindingRoot.resolve("list_container"), "[\"element 1\", \"element 2\"]");
        write(bindingRoot.resolve("int_container"), "1337");
        write(bindingRoot.resolve("token"), "auth-token");

        // setup environment variable reader
        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        // setup subject under test
        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();
        final ServiceBinding serviceBinding = sut.parse("binding", bindingRoot).orElse(null);

        // assert
        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "credentials");

        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getCredentials().get("token")).isEqualTo("auth-token");
        assertThat(serviceBinding.get("list_container")).isEmpty();
        assertThat(serviceBinding.get("int_container")).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void parseIgnoresPropertyWithUnknownFormat( @Nonnull @TempDir final Path rootDirectory )
        throws IOException
    {
        // setup file system
        final Path bindingRoot = rootDirectory.resolve("binding");
        write(
            bindingRoot.resolve(".metadata"),
            "{\n"
                + "    \"metaDataProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"type\",\n"
                + "            \"format\": \"text\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"unknown_property\",\n"
                + "            \"format\": \"unknown\"\n"
                + "        }\n"
                + "    ],\n"
                + "    \"credentialProperties\": [\n"
                + "        {\n"
                + "            \"name\": \"token\",\n"
                + "            \"format\": \"text\"\n"
                + "        }\n"
                + "    ]\n"
                + "}");
        write(bindingRoot.resolve("type"), "xsuaa");
        write(bindingRoot.resolve("unknown_property"), "some value");
        write(bindingRoot.resolve("token"), "auth-token");

        // setup environment variable reader
        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        // setup subject under test
        final ServiceBindingIoMetadataParsingStrategy sut = ServiceBindingIoMetadataParsingStrategy.newDefault();
        final ServiceBinding serviceBinding = sut.parse("binding", bindingRoot).orElse(null);

        // assert
        assertThat(serviceBinding).isNotNull();
        assertThat(serviceBinding.getKeys()).containsExactlyInAnyOrder("type", "credentials");

        assertThat(serviceBinding.getServiceName().orElse(null)).isEqualTo("xsuaa");
        assertThat(serviceBinding.getCredentials().get("token")).isEqualTo("auth-token");
        assertThat(serviceBinding.get("unknown_property")).isEmpty();
    }

    private static void write( @Nonnull final Path filePath, @Nonnull final String content )
    {
        try {
            if( !Files.exists(filePath.getParent()) ) {
                Files.createDirectories(filePath.getParent());
            }

            Files.write(filePath, Collections.singletonList(content), StandardCharsets.UTF_8);
        }
        catch( final IOException e ) {
            fail("Failed to write test file content.", e);
        }
    }
}
