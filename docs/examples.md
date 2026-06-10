# Examples

### Response

The API call returns a JSON response of the following form:
```json
{
  "total": 22,
  "from": 0,
  "to": 10,
  "results": [
    {
      "score": 0.047619047619047616,
      "originalScore": 14.3,
      "result": {
        "id": "eosc-beyond.my-service",
        "name": "My Service",
        "description": "...",
        ...
      },
      "highlights": [
        { "field": "name", "snippet": "My <em>Service</em>" },
        { "field": "description", "snippet": "...provides a <em>service</em> for..." }
      ]
    }
  ],
  "facets": [
    {
      "field": "trl",
      "label": "TRL",
      "values": [
        { "value": "trl-9", "label": "TRL-9", "count": 5 },
        { "value": "trl-8", "label": "TRL-8", "count": 3 }
      ]
    },
    {
      "field": "node",
      "label": "Node",
      "values": [
        { "value": "EOSC-Beyond", "label": "EOSC-Beyond", "pid": "21.T15999/...", "count": 8 }
      ]
    },
    ...
  ],
  "metadata": {
    "nodes": [
      {
        "id": "4",
        "name": "EOSC-Beyond"
      },
      ...
    ]
  }
}
```

where:
- total: Total number of resources matching the search criteria
- from: Starting index in the result set
- to: Number of resources fetched
- results: List of resources
- facets: List of facets
- metadata: Metadata information (e.g. nodes)

### Examples

#### GET with default or specific query params
- [federation/services](http://federatedsearch.service.eosc-beyond.eu/federation/services)
- [federation/services?quantity=1000](http://federatedsearch.service.eosc-beyond.eu/federation/services?quantity=1000)
- [federation/services?quantity=1000&sort=name&order=asc](http://federatedsearch.service.eosc-beyond.eu/federation/services?quantity=1000&sort=name&order=asc)
- [federation/services?quantity=1000&sort=name&order=asc&keyword=egi](http://federatedsearch.service.eosc-beyond.eu/federation/services?quantity=1000&sort=name&order=asc&keyword=egi)

Available query parameters:

| Parameter   | Type    | Default | Description                      |
| ----------- | ------- |---------|----------------------------------|
| `suspended` | boolean | `false` | Filter by suspended resources    |
| `keyword`   | string  | —       | Keyword search                   |
| `from`      | int     | `0`     | Starting index in the result set |
| `quantity`  | int     | `10`    | Number of results to fetch       |
| `sort`      | string  | —       | Field to sort by                 |
| `order`     | string  | `asc`   | Sorting order (`asc` / `desc`)   |


#### GET with specific facet parameters (service specific fields)
- [federation/services?trl=trl-9](http://federatedsearch.service.eosc-beyond.eu/federation/services?trl=trl-9)
- [federation/services?jurisdiction=ds_jurisdiction-global](http://federatedsearch.service.eosc-beyond.eu/federation/services?jurisdiction=ds_jurisdiction-global)
- [federation/services?categories=category-access_physical_and_eInfrastructures-instrument_and_equipment,category-security_and_operations-security_and_identity](http://federatedsearch.service.eosc-beyond.eu/federation/services?categories=category-access_physical_and_eInfrastructures-instrument_and_equipment,category-security_and_operations-security_and_identity)

Available facet parameters:

| Parameter               | Type            | Multiplicity | Example                              |
|-------------------------|-----------------|--------------|--------------------------------------|
| `access_types`          | vocabulary      | 1            | access_type-remote                   |
| `categories`            | vocabulary      | N            | category-other-other                 |
| `jurisdiction`          | vocabulary      | N            | ds_jurisdiction-global               |
| `name`                  | string          | 1            | Service name                         |
| `node`                  | string          | 1            | 21.T15999/EOSC-BEYOND                |
| `order_type`            | vocabulary      | 1            | order_type-fully_open_access         |
| `resource_owner`        | Organisation ID | 1            | 21.T15999/0iCJ9y                     |
| `scientific_domains`    | vocabulary      | N            | scientific_domain-generic            |
| `scientific_subdomains` | vocabulary      | N            | scientific_subdomain-generic-generic |
| `service_providers`     | Organisation ID | N            | 21.T15999/0iCJ9y                     |
| `subcategories`         | vocabulary      | N            | subcategory-other-other-other        |
| `tags`                  | string          | N            | tag1                                 |
| `trl`                   | vocabulary      | 1            | trl-9                                |

---

## List of Vocabularies
- [ACCESS_TYPE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/ACCESS_TYPE.json)
- [CATEGORY](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/CATEGORY.json)
- [JURISDICTION](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/DS_JURISDICTION.json)
- [ORDER_TYPE](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/ORDER_TYPE.json)
- [SCIENTIFIC_DOMAIN](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SCIENTIFIC_DOMAIN.json)
- [SCIENTIFIC_SUBDOMAIN](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SCIENTIFIC_SUBDOMAIN.json)
- [SUBCATEGORY](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/SUBCATEGORY.json)
- [TRL](https://github.com/madgeek-arc/resource-catalogue-docs/blob/master/vocabularies/TRL.json)