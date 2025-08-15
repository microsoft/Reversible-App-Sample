# Reversible App Sample – New README

This sample shows a reversible architecture where you can switch messaging backends without changing application code. Dapr pub/sub abstracts the broker so you can run locally with RabbitMQ or in a cloud-like mode with Azure Service Bus. The startup scripts automatically stop any existing environment and build containers when needed; after that, switching backends reuses the same images.

## What the sample does

- Java services (Spring Boot)
  - `reversible-api` (API): publishes customer events through Dapr
  - `reversible-observer` (Observer): subscribes to the same events via Dapr
- React `customer-webapp` UI to interact with the API
- PostgreSQL for persistence (pre-seeded data)
- Dapr sidecars + components for pub/sub
- Aspire Dashboard for local telemetry

The API emits customer events to the Dapr pub/sub component. The Observer subscribes to the topic and logs what it receives. You can confirm this behavior by tailing the observer container logs.

## Build once, switch freely

- First run of either start script will build the Docker images if they’re missing (no separate build step required).
- When you switch between RabbitMQ and Azure Service Bus, you don’t need to rebuild the images. The Dapr components change, not the Java or React code.
- You don’t need to run a separate stop script—the start scripts stop any existing environment automatically.

## Azure prerequisites (for Azure Service Bus mode)

When running the Azure environment, you need an existing Azure Service Bus with a topic for the sample’s events:

- Create (or reuse) a Service Bus namespace
- Create a Topic named `customer-events` 
- Get the connection string (with Manage or Send/Listen as appropriate)
- Put the connection string in `docker/dapr/secrets.json` under the key `servicebus-connection-string`
  - See `docker/dapr/secrets.json.sample` for the expected JSON structure

Dapr’s Azure Service Bus component (`docker/dapr/components-azure/pubsub.yaml`) reads that secret via the local file secret store (`docker/dapr/components-azure/secrets.yaml`).

## How to start the environments

Prerequisites:
- Windows with PowerShell (pwsh)
- Docker Desktop running

From the repo root, pick one mode:

### Option A: Local with RabbitMQ

1) (Optional) Create secrets file if you don’t have one yet:

```powershell
Copy-Item docker/dapr/secrets.json.sample docker/dapr/secrets.json -ErrorAction SilentlyContinue
# For RabbitMQ mode, you can keep only the Service Bus key empty; RabbitMQ connection is embedded in the component config.
```

2) Start:

```powershell
pwsh ./start-rabbitmq.ps1
```

Services and endpoints:
- Webapp: http://localhost:8083
- API: http://localhost:8081
- Observer: http://localhost:8090
- PostgreSQL: localhost:5432
- RabbitMQ Management: http://localhost:15672 (admin/admin123)
- Aspire Dashboard: http://localhost:18888

### Option B: Cloud-like with Azure Service Bus

1) Create secrets file and set your connection string:

```powershell
Copy-Item docker/dapr/secrets.json.sample docker/dapr/secrets.json
# Edit docker/dapr/secrets.json and set:
# {
#   "servicebus-connection-string": "Endpoint=sb://<yournamespace>.servicebus.windows.net/;SharedAccessKeyName=<name>;SharedAccessKey=<key>"
# }
```

2) Start:

```powershell
pwsh ./start-azure.ps1
```

Services and endpoints:
- Webapp: http://localhost:8083
- API: http://localhost:8081
- Observer: http://localhost:8090
- PostgreSQL: localhost:5432
- Aspire Dashboard: http://localhost:18888

## Seeing the observer consume messages

The observer subscribes to the `customer-events` topic (via Dapr Subscription). To watch it process messages, stream logs from the `observer` container:

```powershell
# Azure Service Bus mode
docker-compose -f docker/docker-compose-azure.yml logs -f observer

# RabbitMQ mode
docker-compose -f docker/docker-compose-rabbitmq.yml logs -f observer
```

You should see log entries whenever you interact with the app (e.g., creating/updating customers through the API/webapp).

## Switching backends

To switch backends, just run the other start script. It will stop the currently running environment automatically and start the selected one:

```powershell
# From Azure -> RabbitMQ
pwsh ./start-rabbitmq.ps1

# From RabbitMQ -> Azure (ensure your Service Bus connection string is set)
pwsh ./start-azure.ps1
```

No application code changes are needed; Dapr components handle the difference.

## Notes and tips

- If a start script fails early, ensure Docker Desktop is running and that required files exist:
  - `docker/dapr/secrets.json` with `servicebus-connection-string` for Azure mode
- Use the Aspire Dashboard at http://localhost:18888 to inspect traces and logs.

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit [Contributor License Agreements](https://cla.opensource.microsoft.com).

When you submit a pull request, a CLA bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft
trademarks or logos is subject to and must follow
[Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/legal/intellectualproperty/trademarks/usage/general).
Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft sponsorship.
Any use of third-party trademarks or logos are subject to those third-party's policies.
