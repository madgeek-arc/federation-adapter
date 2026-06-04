[![EOSC Beyond Logo][eosc-logo]]()

# Federation Search

## Description
The **Federation Search** is a Java-based service that aggregates EOSC Resources from multiple 
**[Resource Catalogue](https://github.com/madgeek-arc/resource-catalogue)** instances into a single API endpoint.

## Installation
```
git clone https://github.com/madgeek-arc/federation-search.git
```

## Build
```
mvn clean install
```

## Configuration

The service supports two modes of endpoint configuration, controlled by the `node.endpoints.manual-config` property in `application.properties`.

### Mode 1: Registry-based (default)

```properties
node.endpoints.manual-config=false
node.registry.url=https://<registry-host>/federation-backend/tenants/<tenant>/nodes
node.registry.key=<api-key>
```

The service fetches the list of node endpoints dynamically from the URL specified in `node.registry.url`. Use `node.registry.key` if the registry requires authentication.

### Mode 2: Manual configuration

```properties
node.endpoints.manual-config=true
```

The service uses the static list of endpoints defined in [node-endpoints.json](src/main/resources/node-endpoints.json). Edit this file to add or remove endpoints:

```json
{
  "endpoints": [
    "https://<node-host>/api"
  ]
}
```

The service appends the resource-type path automatically (e.g. `public/service/search`).

## Run
1. Configure the service as described above.
2. Start the service:
   ```
   java -jar target/search-aggregator-X.X.X-SNAPSHOT.jar
   ```

## API
The service exposes endpoints that query all configured nodes and aggregate the results.

**Endpoints**:
```
GET http://localhost:8080/api/federation/adapters
GET http://localhost:8080/api/federation/catalogues
GET http://localhost:8080/api/federation/datasources
GET http://localhost:8080/api/federation/deployableApplications
GET http://localhost:8080/api/federation/interoperabilityRecords
GET http://localhost:8080/api/federation/organisations
GET http://localhost:8080/api/federation/services
GET http://localhost:8080/api/federation/trainingResources
```

**SwaggerUI**:
```
http://localhost:8080/api/swagger-ui/index.html
```

For more detailed documentation and examples, see [docs/examples.md](docs/examples.md).

[eosc-logo]: https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png
