# bag-service

Backend layer of the [cookie-based per-layer version routing POC](https://github.com/asharma157/bag-ui).
Java 17 / Spring Boot. Returns the hardcoded contents of a customer's bag (cart). No database,
no auth, no persistent state.

**The full POC — the cookie model, the mesh routing, local validation, GKE deployment and the
demo script — is documented in the [bag-ui](https://github.com/asharma157/bag-ui) repo.** This README covers only
what is specific to this service.

## Its version is selected by the `bag_service` cookie

`bag_service=1.10` routes a request to pods labelled `version: 1.10`. Istio does that routing;
nothing here selects a version, and callers always address this service at the constant
`http://bag-service:8080/api/bags`.

The bag contents are a plain hardcoded list in
[`service/BagService.java`](src/main/java/com/example/bagservice/service/BagService.java) —
whatever this branch of the code says they are. `main` currently returns four items totalling
$682.00.

### Shipping a version with different contents

Two versions differ because their code differs. Branch, edit the list, then deploy the branch:

```bash
git checkout -b feature/bag-new
# edit ITEMS in src/main/java/com/example/bagservice/service/BagService.java
git commit -am "new cart contents"
git push -u origin feature/bag-new
```

**Pushing runs CI only — it does not deploy.** CI builds and smoke-tests the branch; no image is
pushed to the registry. To ship it, run the Deploy workflow manually:

```bash
gh workflow run deploy.yml --repo asharma157/bag-service --ref feature/bag-new
```

or in GitHub: **Actions → Deploy → Run workflow →** pick the branch, leave *version* blank.

The version is derived from the branch name, lowercased and made tag-safe, so `feature/bag-new`
ships as version **`feature-bag-new`** — that is the value to put in the cookie. If no
`k8s/deployment-feature-bag-new.yaml` exists, the workflow renders one from the default version's
manifest; commit it if the version is here to stay.

### Deploying a version does not move traffic

New pods join the `bag-service` Service, which spans every version. What keeps the default at
100% is the **catch-all route** at the end of the bag-service VirtualService: unless a request
matches a cookie rule above it, it goes to the default subset. Add a match rule (Kiali, or YAML)
to make `bag_service=feature-bag-new` reach the new pods; until you do, the new version is
deployed and reachable but takes no traffic.

> Without a VirtualService at all, the Service load-balances across every version's pods — so a
> second version would immediately take a share of default traffic. The catch-all is what makes
> "deploy freely, route deliberately" true.

## Structure

One controller, one service:

| File | Role |
|---|---|
| [`web/BagController.java`](src/main/java/com/example/bagservice/web/BagController.java) | `GET /api/bags`, `GET /health` |
| [`service/BagService.java`](src/main/java/com/example/bagservice/service/BagService.java) | the hardcoded item list |
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
| `APP_VERSION` | `1.9` | the version this instance reports; in Kubernetes it comes from the pod's `version` label via the downward API |
| `POD_NAME` | hostname | instance identity, stamped on responses |

The injected version is reported, never consulted: it identifies which build answered a request.
Every response carries `x-bag-service-version` and `x-bag-service-instance`.

## Deploy

```bash
gcloud builds submit . --tag $REPO/bag-service:1.9
sed -i.bak "s#us-central1-docker.pkg.dev/PROJECT_ID/bag-poc#$REPO#g" k8s/deployment-*.yaml
kubectl apply -f k8s/
```

`k8s/` holds one Deployment per version plus a single Service selecting on `app: bag-service`
only, so it spans every version. VirtualService and DestinationRule are managed separately — by
the routing controller or Kiali — and are not part of this repo.
