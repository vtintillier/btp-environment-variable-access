/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.environment.servicebinding.api.ServiceBindingAccessor;
import com.sap.cloud.environment.servicebinding.api.exception.ServiceBindingAccessException;

/**
 * A {@link ServiceBindingAccessor} that is able to load {@link ServiceBinding}s that conform to the
 * <a href="https://servicebinding.io/spec/core/1.0.0/">servicebinding.io</a> specification from the file system. The
 * file system structure is assumed to look as follows:
 *
 * <pre>
 *     $SERVICE-BINDING-ROOT
 *     ├-- {SERVICE-BINDING-NAME#1}
 *     |   ├-- {PROPERTY#1}
 *     |   ├-- ...
 *     |   └-- {PROPERTY#N}
 *     └-- {SERVICE-BINDING-NAME#2}
 *         ├-- {PROPERTY#1}
 *         ├-- ...
 *         └-- {PROPERTY#N}
 * </pre>
 * <p>
 * By default, following {@link ServiceBindingIoParsingStrategy}s are applied:
 * <ol>
 * <li>{@link ServiceBindingIoMetadataParsingStrategy}</li>
 * <li>{@link ServiceBindingIoVanillaParsingStrategy}</li>
 * </ol>
 * The <b>order</b> of the applied strategies <b>is important</b> as only the first parsed value for each service
 * binding will be considered.
 * </p>
 */
public class SapServiceOperatorServiceBindingIoAccessor implements ServiceBindingAccessor
{
    @Nonnull
    private static final Logger logger = LoggerFactory.getLogger(SapServiceOperatorServiceBindingIoAccessor.class);

    /**
     * The default {@link Function} to read environment variables.
     */
    @Nonnull
    public static final Function<String, String> DEFAULT_ENVIRONMENT_VARIABLE_READER = System::getenv;

    /**
     * The default {@link ServiceBindingIoParsingStrategy}s to parse service bindings.
     */
    @Nonnull
    public static final Collection<ServiceBindingIoParsingStrategy> DEFAULT_PARSING_STRATEGIES =
        Collections
            .unmodifiableCollection(
                Arrays
                    .asList(
                        ServiceBindingIoMetadataParsingStrategy.newDefault(),
                        ServiceBindingIoVanillaParsingStrategy.newDefault()));

    @Nonnull
    private static final String ROOT_DIRECTORY_KEY = "SERVICE_BINDING_ROOT";

    @Nonnull
    private final Collection<ServiceBindingIoParsingStrategy> parsingStrategies;

    @Nonnull
    private final Function<String, String> environmentVariableReader;

    /**
     * Initializes a new {@link SapServiceOperatorServiceBindingIoAccessor} instance that uses the
     * {@link #DEFAULT_PARSING_STRATEGIES} and {@link #DEFAULT_ENVIRONMENT_VARIABLE_READER}..
     */
    public SapServiceOperatorServiceBindingIoAccessor()
    {
        this(DEFAULT_PARSING_STRATEGIES, DEFAULT_ENVIRONMENT_VARIABLE_READER);
    }

    /**
     * Initializes a new {@link SapServiceOperatorServiceBindingIoAccessor} instance that uses the given
     * {@code parsingStrategies}, {@code environmentVariableReader}, and {@code charset}.
     *
     * @param parsingStrategies
     *            The {@link ServiceBindingIoParsingStrategy}s that should be used.
     * @param environmentVariableReader
     *            The {@link Function} that should be used to read environment variables.
     */
    public SapServiceOperatorServiceBindingIoAccessor(
        @Nonnull final Collection<ServiceBindingIoParsingStrategy> parsingStrategies,
        @Nonnull final Function<String, String> environmentVariableReader )
    {
        this.parsingStrategies = parsingStrategies;
        this.environmentVariableReader = environmentVariableReader;
    }

    @Nonnull
    @Override
    public List<ServiceBinding> getServiceBindings()
        throws ServiceBindingAccessException
    {
        final Path rootDirectory = getRootDirectory();
        if( rootDirectory == null ) {
            return Collections.emptyList();
        }

        logger.debug("Reading service bindings from '{}'.", rootDirectory);
        try( final Stream<Path> bindingRoots = Files.list(rootDirectory).filter(Files::isDirectory) ) {
            return bindingRoots
                .map(this::parseServiceBinding)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        }
        catch( final IOException e ) {
            return Collections.emptyList();
        }
    }

    @Nullable
    private Path getRootDirectory()
    {
        logger
            .debug(
                "Trying to determine service binding root directory using the '{}' environment variable.",
                ROOT_DIRECTORY_KEY);
        final String maybeRootDirectory = environmentVariableReader.apply(ROOT_DIRECTORY_KEY);
        if( maybeRootDirectory == null || maybeRootDirectory.isEmpty() ) {
            logger.debug("Environment variable '{}' is not defined.", ROOT_DIRECTORY_KEY);
            return null;
        }

        final Path rootDirectory = Paths.get(maybeRootDirectory);
        if( !Files.exists(rootDirectory) || !Files.isDirectory(rootDirectory) ) {
            logger
                .debug(
                    "Environment variable '{}' ('{}') does not point to a valid directory.",
                    ROOT_DIRECTORY_KEY,
                    maybeRootDirectory);
            return null;
        }

        return rootDirectory;
    }

    @Nonnull
    private Optional<ServiceBinding> parseServiceBinding( @Nonnull final Path rootDirectory )
    {
        return parsingStrategies
            .stream()
            .map(strategy -> applyStrategy(strategy, rootDirectory))
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(Optional.empty());
    }

    @Nonnull
    private
        Optional<ServiceBinding>
        applyStrategy( @Nonnull final ServiceBindingIoParsingStrategy strategy, @Nonnull final Path bindingPath )
    {
        try {
            return strategy.parse(bindingPath.getFileName().toString(), bindingPath);
        }
        catch( final IOException e ) {
            return Optional.empty();
        }
    }
}
