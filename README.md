<div align="center">
  <img src="https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png" alt="EOSC Beyond Logo">
</div>

# Federation Adapter

## Description
The **Federation Adapter** is a Java-based service that aggregates Services from multiple 
**[Resource Catalogue](https://github.com/madgeek-arc/resource-catalogue)** instances into a single API endpoint.

## Installation
```
git clone https://github.com/madgeek-arc/federation-adapter.git
```

## Build
```
mvn clean install
```

## Run
1. Edit the configuration file [node-endpoints.json](src/main/resources/node-endpoints.json) to 
   include the node endpoints from which the service should aggregate data.
2. Start the service:
   ```
   java -jar target/adapter-X.X.X-SNAPSHOT.jar
   ```

## API
The service exposes a single API endpoint that queries all configured nodes from
[node-endpoints.json](src/main/resources/node-endpoints.json) and aggregates the results.

**Endpoint**:
```
GET http://localhost:8080/api/federation/services
```

**SwaggerUI**:
```
http://localhost:8080/api/swagger-ui/index.html
```

For more detailed documentation and examples, see [docs/examples.md](docs/examples.md).