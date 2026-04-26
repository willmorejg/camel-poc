# camel-poc

A proof-of-concept application demonstrating [Apache Camel](https://camel.apache.org/) with Spring Boot.

## Stack

- Java 21
- Spring Boot 3.5
- Apache Camel 4.x (YAML DSL)
- Spring Security (JWT + LDAP)
- DuckDB (embedded JDBC)
- Thymeleaf, Flying Saucer / OpenPDF
- OpenAPI / Swagger UI

## Routes

| Route file | Description |
|---|---|
| `csv-to-json.yaml` | Polls a directory for CSV files and converts them to JSON |
| `xml-to-json.yaml` | Polls a directory for XML files and converts them to JSON |
| `xml-to-in.yaml` | SOAP/XML web service — accepts address records and writes them to the inbound directory |
| `rest-to-file.yaml` | REST endpoint that accepts JSON payloads and writes them to the inbound directory |
| `json-to-geocode-to-pdf.yaml` | Picks up JSON address records, geocodes them via Nominatim, renders an HTML map tile via Thymeleaf, and converts to PDF |

## Security

All endpoints require a JWT bearer token except `POST /oauth/token` and the Swagger UI.

Obtain a token:

```bash
curl -s -X POST http://localhost:8080/oauth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"<user>","password":"<password>"}'
```

LDAP is used to authenticate credentials. Default LDAP URL is `ldap://localhost:8389`.

## Key Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/oauth/token` | Obtain a JWT |
| `POST` | `/v1/api/addresses` | Submit JSON address records |
| `POST` | `/v1/ws/address-records` | Submit SOAP/XML address records |
| `GET`  | `/v1/ws/address-records?wsdl` | Retrieve the service WSDL |
| `GET`  | `/swagger-ui.html` | Swagger UI |

## File Paths

Configured via `camel.variables` in `application.yml`:

| Variable | Default | Purpose |
|---|---|---|
| `jsonInputPath` | `/data/in` | Inbound JSON drop folder |
| `jsonToProcessPath` | `/data/json-to-process` | Files staged for processing |
| `jsonToHtmlPath` | `/data/json-to-html` | Rendered HTML output |
| `pdfOutputPath` | `/data/pdf` | Generated PDF output |
| `geoJsonInputPath` | `/data/geo/in` | Inbound GeoJSON |
| `jsonErrorPath` | `/data/error` | Files that failed processing |

## Running

```bash
./gradlew bootRun
```

A Docker Compose file (`compose.yml`) is provided for supporting services (LDAP, PostgreSQL, ActiveMQ).

```bash
docker compose up -d
```

## Configuration

Copy or override values in `application.yml`. Key environment variables:

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (base64 key) | HS256 signing secret (min 32 bytes) |
| `SPRING_LDAP_URLS` | `ldap://localhost:8389` | LDAP server |
| `SPRING_LDAP_BASE` | `dc=ljcomputing,dc=net` | LDAP base DN |
| `SPRING_LDAP_USERNAME` | `cn=admin,...` | LDAP bind DN |
| `SPRING_LDAP_PASSWORD` | `admin` | LDAP bind password |

## License

Apache License 2.0 — see [LICENSE](LICENSE).
