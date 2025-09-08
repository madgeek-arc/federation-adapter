### Response

The API call returns a JSON response of the following form:
```json
{
  "total": 100,
  "from": 0,
  "to": 10,
  "results": [
    {},
    {}
  ],
  "facets": [
    {},
    {}
  ]
}
```

where:
- total: Number of total Services that matches the search criteria
- from: Starting index in the result set
- to: Number of Services fetched
- results: List of [Services](service_example.json)
- facets: List of [Facets](facet_example.json)

### Examples

#### GET with default or specific query params
- [api/federation/services](http://79.129.71.168/api/federation/services)
- [api/federation/services?quantity=1000](http://79.129.71.168/api/federation/services?quantity=1000)
- [api/federation/services?quantity=1000&sort=name&order=asc](http://79.129.71.168/api/federation/services?quantity=1000&sort=name&order=asc)
- [api/federation/services?quantity=1000&sort=name&order=asc&keyword=egi](http://79.129.71.168/api/federation/services?quantity=1000&sort=name&order=asc&keyword=egi)

Available query parameters:

| Parameter   | Type    | Default | Description                      |
| ----------- | ------- |---------|----------------------------------|
| `suspended` | boolean | false   | Filter by suspended services     |
| `keyword`   | string  | —       | Keyword search                   |
| `from`      | int     | 0       | Starting index in the result set |
| `quantity`  | int     | 10      | Number of results to fetch       |
| `sort`      | string  | -       | Field to sort by                 |
| `order`     | string  | asc     | Sorting order (`asc` / `desc`)   |


#### GET with specific facet parameters (service specific fields)
- [api/federation/services?resource_organisation=21.T15999/C2FiOh](http://79.129.71.168/api/federation/services?resource_organisation=21.T15999/C2FiOh)
- [api/federation/services?resource_geographic_locations=RS,DE](http://79.129.71.168/api/federation/services?resource_geographic_locations=RS,DE)

Available facet parameters:

| Parameter                        | Type         | Multiplicity | Example                                         |
|----------------------------------|--------------|--------------|-------------------------------------------------|
| `abbreviation`                   | string       | 1            | Service abbreviation                            |
| `access_modes`                   | vocabulary   | N            | access_mode-peer_reviewed                       |
| `access_types`                   | vocabulary   | N            | access_type-remote                              |
| `alternative_identifiers_values` | string       | N            | test identifier                                 |
| `catalogue_id`                   | Catalogue ID | 1            | eosc                                            |
| `categories`                     | vocabulary   | N            | -                                               |
| `description`                    | string       | 1            | Service description                             |
| `funding_body`                   | vocabulary   | N            | funding_body-esa                                |
| `funding_programs`               | vocabulary   | N            | funding_program-h2020                           |
| `geographical_availabilities`    | vocabulary   | N            | WW (Worldwide)                                  |
| `language_availabilities`        | vocabulary   | N            | de (German)                                     |
| `life_cycle_status`              | vocabulary   | 1            | life_cycle_status-production                    |
| `marketplace_locations`          | vocabulary   | N            | marketplace_location-manage_research_data       |
| `name`                           | string       | 1            | Service name                                    |
| `node`                           | vocabulary   | 1            | node-sandbox                                    |
| `open_source_technologies`       | string       | N            | Docker                                          |
| `order_type`                     | vocabulary   | 1            | order_type-fully_open_access                    |
| `resource_geographic_locations`  | vocabulary   | N            | RS (Serbia)                                     |
| `resource_organisation`          | Provider ID  | 1            | 21.T15999/0iCJ9y                                |
| `resource_providers`             | Provider ID  | N            | 21.T15999/0iCJ9y                                |
| `scientific_domains`             | vocabulary   | N            | -                                               |
| `scientific_subdomains`          | vocabulary   | N            | scientific_subdomain-generic-generic            |
| `service_categories`             | vocabulary   | N            | service_category-storage                        |
| `subcategories`                  | vocabulary   | N            | subcategory-sharing_and_discovery-data-metadata |
| `tagline`                        | string       | 1            | Service tagline                                 |
| `tags`                           | string       | N            | tag1                                            |
| `target_users`                   | vocabulary   | N            | target_user-research_managers                   |
| `trl`                            | vocabulary   | 1            | trl-9                                           |

---

## List of Vocabularies
- [ACCESS_MODE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/ACCESS_MODE.json)
- [ACCESS_TYPE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/ACCESS_TYPE.json)
- [COUNTRY](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/COUNTRY.json)
- [FUNDING_BODY](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/FUNDING_BODY.json)
- [FUNDING_PROGRAM](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/FUNDING_PROGRAM.json)
- [GEOGRAPHIC_LOCATION](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/GEOGRAPHIC_LOCATION.json)
- [LANGUAGE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/LANGUAGE.json)
- [LIFE_CYCLE_STATUS](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/LIFE_CYCLE_STATUS.json)
- [MARKETPLACE_LOCATION](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/MARKETPLACE_LOCATION.json)
- [NODE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/NODE.json)
- [ORDER_TYPE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/ORDER_TYPE.json)
- [REGION](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/REGION.json)
- [SCIENTIFIC_SUBDOMAIN](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SCIENTIFIC_SUBDOMAIN.json)
- [SERVICE_CATEGORY](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SERVICE_CATEGORY.json)
- [SERVICE_TYPE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SERVICE_TYPE.json)
- [SUBCATEGORY](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SUBCATEGORY.json)
- [TARGET_USER](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/TARGET_USER.json)
- [TRL](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/TRL.json)