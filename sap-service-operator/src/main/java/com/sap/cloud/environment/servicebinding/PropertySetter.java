/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.servicebinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FunctionalInterface
interface PropertySetter
{
    @Nonnull
    String CREDENTIALS_KEY = "credentials";

    @Nonnull
    @SuppressWarnings( "unchecked" )
    PropertySetter TO_CREDENTIALS = ( binding, name, value ) -> {
        Map<String, Object> credentials = null;
        if( binding.containsKey(CREDENTIALS_KEY) ) {
            final Object maybeCredentials = binding.get(CREDENTIALS_KEY);
            if( maybeCredentials instanceof Map ) {
                credentials = (Map<String, Object>) maybeCredentials;
            } else {
                throw new IllegalStateException(
                    String.format("The '%s' property must be of type %s.", CREDENTIALS_KEY, Map.class.getSimpleName()));
            }
        }

        if( credentials == null ) {
            credentials = new HashMap<>();
            binding.put(CREDENTIALS_KEY, credentials);
        }

        credentials.put(name, value);
    };

    @Nonnull
    PropertySetter TO_ROOT = Map::put;

    @Nonnull
    @SuppressWarnings( "unchecked" )
    static PropertySetter asList( @Nonnull final PropertySetter actualSetter )
    {
        return ( binding, name, value ) -> {
            final List<Object> list;
            if( value instanceof List ) {
                list = (List<Object>) value;
            } else if( value instanceof String ) {
                list = new JSONArray((String) value).toList();
            } else {
                throw new IllegalStateException(
                    String
                        .format(
                            "The provided value '%s' cannot be converted to a %s.",
                            value,
                            List.class.getSimpleName()));
            }

            actualSetter.setProperty(binding, name, list);
        };
    }

    @Nonnull
    static PropertySetter asJson( @Nonnull final PropertySetter actualSetter )
    {
        return ( binding, name, value ) -> {
            // Wrap the property value inside another JSON object.
            // This way we don't have to manually take care of correctly parsing the property type
            // (it could be an integer, a boolean, a list, or even an entire JSON object).
            // Instead, we can delegate the parsing logic to our JSON library.
            final String jsonWrapper = String.format("{\"content\": %s}", value);

            final JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonWrapper);
            }
            catch( final JSONException e ) {
                throw new IllegalStateException(
                    String.format("The provided value '%s' cannot be converted into a valid JSON object.", value));
            }

            final Object content = jsonObject.get("content");
            if( content instanceof JSONObject ) {
                actualSetter.setProperty(binding, name, ((JSONObject) content).toMap());
            } else if( content instanceof JSONArray ) {
                actualSetter.setProperty(binding, name, ((JSONArray) content).toList());
            } else {
                actualSetter.setProperty(binding, name, content);
            }
        };
    }

    void setProperty(
        @Nonnull final Map<String, Object> rawServiceBinding,
        @Nonnull final String propertyName,
        @Nonnull final Object propertyValue );
}
