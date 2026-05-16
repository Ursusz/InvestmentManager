# Investment Manager Frontend

Static frontend served by Nginx. Nginx also proxies `/api/*` requests to the backend container, so the browser does not need CORS configuration.

## Build

```bash
docker build -t investment-manager-frontend .
```

## Run Against A Backend On The Host

If your Spring Boot backend runs on your machine at `http://localhost:8080`:

```bash
docker run --rm -p 3000:80 -e BACKEND_URL=http://host.docker.internal:8080 investment-manager-frontend
```

Open:

```text
http://localhost:3000
```

## Run With Backend In Another Docker Container

Create a shared network:

```bash
docker network create investment-manager-net
```

Run the backend container on the same network with the name `backend`, then run the frontend:

```bash
docker run --rm --name frontend --network investment-manager-net -p 3000:80 -e BACKEND_URL=http://backend:8080 investment-manager-frontend
```

The frontend calls:

```text
/api/deposit
/api/withdraw
/api/generateAccountSatement
/api/resetAccount
/api/backFillMonthlyInterest
```

Deposits and withdrawals refresh the visible table from `/api/generateAccountSatement` after the API call completes. The frontend does not create transaction rows locally; the table only shows transactions returned by the backend.

Nginx forwards those to:

```text
$BACKEND_URL/deposit
$BACKEND_URL/withdraw
$BACKEND_URL/generateAccountSatement
$BACKEND_URL/resetAccount
$BACKEND_URL/backFillMonthlyInterest
```

Note: the backend endpoint is currently spelled `generateAccountSatement`, so the frontend uses that exact spelling.
