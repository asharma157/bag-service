# bag-service

Backend layer of the [cookie-based per-layer version routing POC](../bag-ui/README.md).
Java 17 / Spring Boot. Returns the hardcoded contents of a customer's bag (cart). No database,
no auth, no persistent state.

**The full POC — the cookie model, the mesh routing, local validation, GKE deployment and the
demo script — is documented in the [bag-ui](../bag-ui/README.md) repo.** This README covers only
what is specific to this service.

## Its version is selected by the `bag_service` cookie

`bag_service=1.10` routes a request to pods labelled `version: 1.10`. Istio does that routing;
nothing here selects a version, and callers always address this service at the constant
`http://bag-service:8080/api/bags`.

Versions shipped, each returning a visibly different item list so routing is verifiable at a
glance:

| Version | Items | Subtotal |
|---|---|---|
| `1.9` (default) | 3 — tote, crossbody, backpack | $467.00 |
| `1.10` | 4 — adds a Weekender Duffel, repriced tote | $662.00 |
| `feature1` | 2 — monogrammed tote, gift pouch | $256.00 |

The lists live in [`service/BagService.java`](src/main/java/com/example/bagservice/service/BagService.java),
keyed by version, so one image can be deployed as any version and the POC stays cheap to run. In
a real pipeline each version is a distinct build of a distinct source revision and that map is
simply the difference between two commits. The pod's `version` label picks the list — never
anything in the request.

## Structure

One controller, one service:

| File | Role |
|---|---|
| [`web/BagController.java`](src/main/java/com/example/bagservice/web/BagController.java) | `GET /api/bags`, `GET /health` |
| [`service/BagService.java`](src/main/java/com/example/bagservice/service/BagService.java) | the hardcoded item lists |
| [`routing/RoutingContextFilter.java`](src/main/java/com/example/bagservice/routing/RoutingContextFilter.java) | captures the routing context |

This is the last hop, so it has nothing to propagate to — instead it **echoes back** the routing
context it received, as `routingContextReceived` in the response body. If `bag_fed`, `bag_orch`
and `bag_service` all arrive here intact, every layer above forwarded them correctly. That echo
is the end-to-end propagation proof the whole POC rests on.

## Run it

```bash
mvn package -DskipTests
APP_VERSION=1.9 java -jar target/bag-service-0.0.1-SNAPSHOT.jar --server.port=8082
```

| Env var | Default | Purpose |
|---|---|---|
| `APP_VERSION` | `1.9` | the version this instance reports and serves; in Kubernetes it comes from the pod's `version` label via the downward API |
| `POD_NAME` | hostname | instance identity, stamped on responses |

A pod labelled with a version that has no item list of its own serves the `1.9` list and reports
the mismatch as `catalogueVersion` rather than failing. Every response carries
`x-bag-service-version` and `x-bag-service-instance`.

## Deploy

```bash
gcloud builds submit . --tag $REPO/bag-service:1.9
sed -i.bak "s#us-central1-docker.pkg.dev/PROJECT_ID/bag-poc#$REPO#g" k8s/deployment-*.yaml
kubectl apply -f k8s/
```

`k8s/` holds one Deployment per version plus a single Service selecting on `app: bag-service`
only, so it spans every version. VirtualService and DestinationRule are managed separately — by
the routing controller or Kiali — and are not part of this repo.
