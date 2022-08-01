/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import static com.sap.cloud.environment.servicebinding.SapServiceOperatorServiceBindingIoAccessor.DEFAULT_PARSING_STRATEGIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SapServiceOperatorServiceBindingIoAccessorTest
{
    @Test
    void defaultConstructorExists()
    {
        assertThat(new SapServiceOperatorServiceBindingIoAccessor()).isNotNull();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void getServiceBindingsReadsEnvironmentVariable()
    {
        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(any())).thenReturn(null);

        final SapServiceOperatorServiceBindingIoAccessor sut =
            new SapServiceOperatorServiceBindingIoAccessor(DEFAULT_PARSING_STRATEGIES, reader);

        sut.getServiceBindings();

        verify(reader, times(1)).apply(eq("SERVICE_BINDING_ROOT"));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void parseMixedServiceBindings()
    {
        final Path rootDirectory =
            TestResource.get(SapServiceOperatorServiceBindingIoAccessorTest.class, "ValidMixedBindings");

        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        final SapServiceOperatorServiceBindingIoAccessor sut =
            new SapServiceOperatorServiceBindingIoAccessor(DEFAULT_PARSING_STRATEGIES, reader);

        final List<ServiceBinding> serviceBindings = sut.getServiceBindings();

        assertThat(serviceBindings.stream().map(ServiceBinding::getName).filter(Optional::isPresent).map(Optional::get))
            .containsExactlyInAnyOrder("data-xsuaa-binding", "secret-key-xsuaa-binding", "servicebinding-io-binding");
    }

    @SuppressWarnings( "unchecked" )
    @Test
    void brokenBindingsAreIgnored()
    {
        final Path rootDirectory =
            TestResource.get(SapServiceOperatorServiceBindingIoAccessorTest.class, "PartiallyValidMixedBindings");

        final Function<String, String> reader = mock(Function.class);
        when(reader.apply(eq("SERVICE_BINDING_ROOT"))).thenReturn(rootDirectory.toString());

        final SapServiceOperatorServiceBindingIoAccessor sut =
            new SapServiceOperatorServiceBindingIoAccessor(DEFAULT_PARSING_STRATEGIES, reader);

        final List<ServiceBinding> serviceBindings = sut.getServiceBindings();

        assertThat(serviceBindings.stream().map(ServiceBinding::getName).filter(Optional::isPresent).map(Optional::get))
            .containsExactlyInAnyOrder(
                "data-xsuaa-binding",
                "no-metadata-file",
                "no-type-property",
                "secret-key-xsuaa-binding");
    }
}
