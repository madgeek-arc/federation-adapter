<div align="center">
  <img src="https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png" alt="EOSC Beyond Logo">
</div>

# Federation Adapter

## Description
The **Federation Adapter** is a Java-based service that aggregates Services from multiple **Service Catalogue** 
instances into a single API endpoint.

## Installation
```
git clone https://github.com/madgeek-arc/federation-adapter.git
```

## Build
```
mvn clean install
```

## Run
1. Edit the configuration file [node-endpoints.json](federation-adapter/src/main/resources/node-endpoints.json) to 
   include the node endpoints from which the service should aggregate data.
2. Start the service:
   ```
   java -jar target/adapter-X.X.X-SNAPSHOT.jar
   ```

## API
The service exposes a single API endpoint that queries all configured nodes from
[node-endpoints.json](federation-adapter/src/main/resources/node-endpoints.json) and aggregates the results.

**Endpoint**:
```
GET http://localhost:8080/api/federation/services
```

**SwaggerUI**:
```
http://localhost:8080/api/swagger-ui/index.html
```

### Query Parameters
The Federation Adapter supports the same query parameters as the Service Catalogue:

| Parameter   | Type    | Default | Description                      |
| ----------- | ------- | ------- | -------------------------------- |
| `suspended` | boolean | false   | Filter by suspended services     |
| `keyword`   | string  | —       | Keyword search                   |
| `from`      | int     | 0       | Starting index in the result set |
| `quantity`  | int     | 10      | Number of results to fetch       |
| `sort`      | string  | asc     | Sorting order (`asc` / `desc`)   |
| `order`     | string  | —       | Field to sort by                 |


Apart from predefined parameters, you can filter services using most of the service-specific fields.
For example, to fetch all services from a specific provider:
http://localhost:8080/api/federation/services?resource_organisation={providerId}