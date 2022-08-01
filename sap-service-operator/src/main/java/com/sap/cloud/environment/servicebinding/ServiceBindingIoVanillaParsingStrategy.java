/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.environment.servicebinding.api.DefaultServiceBinding;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * A {@link ServiceBindingIoParsingStrategy} that strictly adheres to the
 * <a href="https://servicebinding.io/spec/core/1.0.0/">servicebinding.io specification (version 1.0.0)</a>.
 */
public final class ServiceBindingIoVanillaParsingStrategy implements ServiceBindingIoParsingStrategy
{
    @Nonnull
    private static final Logger logger = LoggerFactory.getLogger(ServiceBindingIoMetadataParsingStrategy.class);

    /**
     * The default {@link Charset} to decode property files.
     */
    @Nonnull
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Nonnull
    private static final String SERVICE_NAME_KEY = "type";

    @Nonnull
    private static final Map<String, PropertySetter> DEFAULT_PROPERTY_SETTERS;

    @Nonnull
    private static final PropertySetter DEFAULT_FALLBACK_PROPERTY_SETTER =
        PropertySetter.asJson(PropertySetter.TO_CREDENTIALS);

    static {
        final Map<String, PropertySetter> defaultPropertySetters = new TreeMap<>(String::compareToIgnoreCase);
        defaultPropertySetters.put(SERVICE_NAME_KEY, PropertySetter.TO_ROOT);
        defaultPropertySetters.put("provider", PropertySetter.TO_ROOT);
        defaultPropertySetters.put("host", PropertySetter.TO_CREDENTIALS);
        defaultPropertySetters.put("port", PropertySetter.asJson(PropertySetter.TO_CREDENTIALS));
        defaultPropertySetters.put("uri", PropertySetter.TO_CREDENTIALS);
        defaultPropertySetters.put("username", PropertySetter.TO_CREDENTIALS);
        defaultPropertySetters.put("password", PropertySetter.TO_CREDENTIALS);
        defaultPropertySetters.put("certificates", PropertySetter.asList(PropertySetter.TO_CREDENTIALS));
        defaultPropertySetters.put("private-key", PropertySetter.TO_CREDENTIALS);

        DEFAULT_PROPERTY_SETTERS = Collections.unmodifiableMap(defaultPropertySetters);
    }

    @Nonnull
    private final Charset charset;

    @Nonnull
    private final Map<String, PropertySetter> propertySetters;

    @Nonnull
    private final PropertySetter fallbackPropertySetter;

    private ServiceBindingIoVanillaParsingStrategy(
        @Nonnull final Charset charset,
        @Nonnull final Map<String, PropertySetter> propertySetters,
        @Nonnull final PropertySetter fallbackPropertySetter )
    {
        this.charset = charset;
        this.propertySetters = propertySetters;
        this.fallbackPropertySetter = fallbackPropertySetter;
    }

    /**
     * Initializes a new {@link ServiceBindingIoVanillaParsingStrategy} instance using the {@link #DEFAULT_CHARSET}.
     *
     * @return A new default {@link ServiceBindingIoVanillaParsingStrategy} instance.
     */
    @Nonnull
    public static ServiceBindingIoVanillaParsingStrategy newDefault()
    {
        return new ServiceBindingIoVanillaParsingStrategy(
            DEFAULT_CHARSET,
            DEFAULT_PROPERTY_SETTERS,
            DEFAULT_FALLBACK_PROPERTY_SETTER);
    }

    @Nonnull
    @Override
    public Optional<ServiceBinding> parse( @Nonnull String bindingName, @Nonnull Path bindingPath )
        throws IOException
    {
        logger.debug("Trying to read service binding from '{}'.", bindingPath);
        final Path metadataFile = bindingPath.resolve(ServiceBindingIoMetadataParsingStrategy.METADATA_FILE);
        if( Files.exists(metadataFile) ) {
            logger
                .debug(
                    "Skipping '{}': The directory contains a '{}' file.",
                    bindingPath,
                    ServiceBindingIoMetadataParsingStrategy.METADATA_FILE);
            return Optional.empty();
        }

        final List<Path> propertyFiles =
            Files.list(bindingPath).filter(Files::isRegularFile).collect(Collectors.toList());

        if( propertyFiles.isEmpty() ) {
            // service binding directory must contain at least one file
            logger.debug("Skipping '{}': The directory is empty.", bindingPath);
            return Optional.empty();
        }

        final Map<String, Object> rawServiceBinding = new HashMap<>();
        for( final Path propertyFile : propertyFiles ) {
            final String propertyName = propertyFile.getFileName().toString();
            final String fileContent = String.join("\n", Files.readAllLines(propertyFile, charset));
            if( fileContent.isEmpty() ) {
                logger.debug("Ignoring empty property file '{}'.", propertyFile);
                continue;
            }

            getPropertySetter(propertyName).setProperty(rawServiceBinding, propertyName, fileContent);
        }

        if( !rawServiceBinding.containsKey(PropertySetter.CREDENTIALS_KEY) ) {
            // service bindings must contain credentials
            logger.debug("Skipping '{}': No credentials property found.", bindingPath);
            return Optional.empty();
        }

        if( !rawServiceBinding.containsKey(SERVICE_NAME_KEY) ) {
            // as per servicebinding.io specification, the binding SHOULD contain a `type` property
            logger.warn("The service binding at '{}' does not contain a {} property.", bindingName, SERVICE_NAME_KEY);
        }

        final DefaultServiceBinding serviceBinding =
            DefaultServiceBinding
                .builder()
                .copy(rawServiceBinding)
                .withName(bindingName)
                .withServiceNameKey(SERVICE_NAME_KEY)
                .withCredentialsKey(PropertySetter.CREDENTIALS_KEY)
                .build();
        logger.debug("Successfully read service binding from '{}'.", bindingPath);
        return Optional.of(serviceBinding);
    }

    @Nonnull
    private PropertySetter getPropertySetter( @Nonnull final String propertyName )
    {
        return propertySetters.getOrDefault(propertyName, fallbackPropertySetter);
    }
}
