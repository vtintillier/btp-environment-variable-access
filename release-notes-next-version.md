## Module: `java-access-api`

* **Breaking Changes:**
  * Removed the default constructor of `com.sap.cloud.environment.servicebinding.api.exception.ServiceBindingAccessException`.

## Module: `java-consumption-api`

* `TypedMapView#getEntries` now also returns all entries that are subtypes (`Class#isAssignableFrom`) of the queried entry type.
* `TypedListView#getItems` now also returns all items that are subtypes (`Class#isAssignableFrom`) of the queried item type.
* **Breaking Changes:**
  * Removed the default constructor of `com.sap.cloud.environment.servicebinding.api.exception.ValueCastException`.
  * Removed the default constructor of `com.sap.cloud.environment.servicebinding.api.exception.KeyNotFoundException`.

## Module: `java-sap-service-operator`

* **Breaking Changes:**
  * The `SapServiceOperatorServiceBindingIoAccessor` has been extended to also cover "vanilla" [servicebinding.io](https://servicebinding.io/spec/core/1.0.0/) (i.e. without the `.metadata` file) service bindings.
    * This led to an incompatible change in the constructor
  * Following classes have been removed from the public API:
    * `com.sap.cloud.environment.servicebinding.metadata.BindingMetadata`
    * `com.sap.cloud.environment.servicebinding.metadata.BindingMetadataFactory`
    * `com.sap.cloud.environment.servicebinding.metadata.BindingProperty`
    * `com.sap.cloud.environment.servicebinding.metadata.PropertyFormat`
  * Following constants have been removed:
    * `com.sap.cloud.environment.servicebinding:SapServiceOperatorServiceBindingIoAccessor#DEFAULT_CHARSET`
