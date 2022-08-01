/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.environment.servicebinding.api.DefaultServiceBinding;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * A {@link ServiceBindingIoParsingStrategy} that expects a {@code .metadata} file (see
 * <a href="https://blogs.sap.com/2022/07/12/the-new-way-to-consume-service-bindings-on-kyma-runtime/">the SAP
 * servicebinding.io specification extension</a>) to exist.
 */
public final class ServiceBindingIoMetadataParsingStrategy implements ServiceBindingIoParsingStrategy
{
    @Nonnull
    private static final Logger logger = LoggerFactory.getLogger(ServiceBindingIoMetadataParsingStrategy.class);

    /**
     * The default {@link Charset} to decode property files.
     */
    @Nonnull
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Nonnull
    static final String METADATA_FILE = ".metadata";

    @Nonnull
    private static final String SERVICE_NAME_KEY = "type";

    @Nonnull
    private static final String TAGS_KEY = "tags";

    @Nonnull
    private static final String PLAN_KEY = "plan";

    @Nonnull
    private static final String CREDENTIALS_KEY = "credentials";

    @Nonnull
    private final Charset charset;

    private ServiceBindingIoMetadataParsingStrategy( @Nonnull Charset charset )
    {
        this.charset = charset;
    }

    /**
     * Initializes a new {@link ServiceBindingIoMetadataParsingStrategy} instance using the {@link #DEFAULT_CHARSET}.
     *
     * @return A new default {@link ServiceBindingIoMetadataParsingStrategy} instance.
     */
    @Nonnull
    public static ServiceBindingIoMetadataParsingStrategy newDefault()
    {
        return new ServiceBindingIoMetadataParsingStrategy(DEFAULT_CHARSET);
    }

    @Nonnull
    @Override
    public Optional<ServiceBinding> parse( @Nonnull String bindingName, @Nonnull Path bindingPath )
        throws IOException
    {
        logger.debug("Trying to read service binding from '{}'.", bindingPath);
        final Optional<BindingMetadata> maybeBindingMetadata = tryParseMetadata(bindingPath);
        if( !maybeBindingMetadata.isPresent() ) {
            // metadata file cannot be parsed
            logger.debug("Skipping '{}': Unable to parse the '{}' file.", bindingPath, METADATA_FILE);
            return Optional.empty();
        }

        final BindingMetadata bindingMetadata = maybeBindingMetadata.get();
        final Map<String, Object> rawServiceBinding = new HashMap<>();
        for( final BindingProperty metadataProperty : bindingMetadata.getMetadataProperties() ) {
            addProperty(rawServiceBinding, bindingPath, metadataProperty);
        }

        final Map<String, Object> rawCredentials = new HashMap<>();
        for( final BindingProperty credentialProperty : bindingMetadata.getCredentialProperties() ) {
            addProperty(rawCredentials, bindingPath, credentialProperty);
        }
        if( rawCredentials.isEmpty() ) {
            // bindings must always have credentials
            logger.debug("Skipping '{}': No credentials property found.", bindingPath);
            return Optional.empty();
        }

        String credentialsKey = CREDENTIALS_KEY;
        if( rawServiceBinding.containsKey(credentialsKey) ) {
            credentialsKey = generateNewKey(rawServiceBinding);
        }

        rawServiceBinding.put(credentialsKey, rawCredentials);

        final DefaultServiceBinding.TerminalBuilder builder =
            DefaultServiceBinding
                .builder()
                .copy(rawServiceBinding)
                .withName(bindingName)
                .withTagsKey(TAGS_KEY)
                .withServicePlanKey(PLAN_KEY)
                .withCredentialsKey(credentialsKey);

        final Optional<String> maybeServiceName = getServiceName(rawServiceBinding);
        maybeServiceName.map(builder::withServiceNameKey);
        if( !maybeServiceName.isPresent() ) {
            // as per servicebinding.io specification, the binding SHOULD contain a `type` property
            logger.warn("The service binding at '{}' does not contain a {} property.", bindingName, SERVICE_NAME_KEY);
        }

        logger.debug("Successfully read service binding from '{}'.", bindingPath);
        return Optional.of(builder.build());
    }

    private Optional<BindingMetadata> tryParseMetadata( @Nonnull final Path bindingPath )
    {
        final Path metadataFile = bindingPath.resolve(METADATA_FILE);
        if( !Files.exists(metadataFile) || !Files.isRegularFile(metadataFile) ) {
            // every service binding must contain a metadata file
            logger.debug("Skipping '{}': The directory does not contain a '{}' file.", bindingPath, METADATA_FILE);
            return Optional.empty();
        }

        return BindingMetadataFactory.tryFromJsonFile(metadataFile);
    }

    private void addProperty(
        @Nonnull final Map<String, Object> properties,
        @Nonnull final Path rootDirectory,
        @Nonnull final BindingProperty property )
    {
        final Optional<String> maybeValue = getPropertyFilePath(rootDirectory, property).flatMap(this::readFile);
        if( !maybeValue.isPresent() ) {
            return;
        }

        final String value = maybeValue.get();

        switch( property.getFormat() ) {
            case TEXT: {
                addTextProperty(properties, property, value);
                break;
            }
            case JSON: {
                addJsonProperty(properties, property, value);
                break;
            }
            default: {
                throw new IllegalStateException(
                    String.format("The format '%s' is currently not supported", property.getFormat()));
            }
        }
    }

    @Nonnull
    private
        Optional<Path>
        getPropertyFilePath( @Nonnull final Path bindingPath, @Nonnull final BindingProperty property )
    {
        final Path propertyFile = bindingPath.resolve(property.getSourceName());
        if( !Files.exists(propertyFile) || !Files.isRegularFile(propertyFile) ) {
            return Optional.empty();
        }

        return Optional.of(propertyFile);
    }

    @Nonnull
    private Optional<String> readFile( @Nonnull final Path path )
    {
        try {
            return Optional.of(String.join("\n", Files.readAllLines(path, charset)));
        }
        catch( final IOException e ) {
            return Optional.empty();
        }
    }

    private void addTextProperty(
        @Nonnull final Map<String, Object> properties,
        @Nonnull final BindingProperty property,
        @Nonnull final String propertyValue )
    {
        properties.put(property.getName(), propertyValue);
    }

    private void addJsonProperty(
        @Nonnull final Map<String, Object> properties,
        @Nonnull final BindingProperty property,
        @Nonnull final String propertyValue )
    {
        // Wrap the property value inside another JSON object.
        // This way we don't have to manually take care of correctly parsing the property type
        // (it could be an integer, a boolean, a list, or even an entire JSON object).
        // Instead, we can delegate the parsing logic to our JSON library.
        final String jsonWrapper = String.format("{\"content\": %s}", propertyValue);

        final JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonWrapper);
        }
        catch( final JSONException e ) {
            return;
        }

        if( !property.isContainer() ) {
            // property is not a container, so the content should be attached as a flat value
            properties.put(property.getName(), jsonObject.get("content"));
            return;
        }

        // the property is a container, so we need to unpack it
        final JSONObject content = jsonObject.optJSONObject("content");
        if( content == null ) {
            // the provided value is not a JSON object
            return;
        }

        for( final String key : content.keySet() ) {
            properties.put(key, content.get(key));
        }
    }

    @Nonnull
    private Optional<String> getServiceName( @Nonnull final Map<String, Object> rawServiceBinding )
    {
        final Object maybeValue = rawServiceBinding.get(SERVICE_NAME_KEY);
        if( !(maybeValue instanceof String) ) {
            return Optional.empty();
        }

        return Optional.of((String) maybeValue);
    }

    @Nonnull
    private String generateNewKey( @Nonnull final Map<String, Object> map )
    {
        for( int i = 0; i < 100; ++i ) {
            final String key = UUID.randomUUID().toString();
            if( map.containsKey(key) ) {
                continue;
            }

            return key;
        }

        throw new IllegalStateException("Unable to generate a new random key. This should never happen!");
    }
}
