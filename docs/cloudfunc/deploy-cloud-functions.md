# Deploying Firebase Cloud Functions

## Prerequisites

1. **Node.js 22** — matches the runtime in `functions/package.json`
2. **Firebase CLI** — install globally:
   ```bash
   npm install -g firebase-tools
   ```
3. **Authenticated session**:
   ```bash
   firebase login
   ```
4. **Correct project selected** (project ID: `pleasest-e3424`):
   ```bash
   firebase use pleasest-e3424
   ```
   Or check current project:
   ```bash
   firebase projects:list
   ```

## Project Structure

```
PlzStop/
├── firebase.json          # Firebase config — declares functions source
├── functions/
│   ├── package.json       # Dependencies, Node 22 engine, build/deploy scripts
│   ├── tsconfig.json      # TypeScript config, outputs to lib/
│   ├── src/
│   │   └── index.ts       # Cloud function source (analyzeReceipt)
│   └── lib/               # Compiled JS output (gitignored)
```

## Deploy Steps

### 1. Install dependencies (first time or after changing deps)

```bash
cd functions
npm install
```

### 2. Build TypeScript

```bash
npm run build
```

This runs `tsc` and outputs compiled JS to `functions/lib/`.

### 3. Deploy

```bash
# From project root
firebase deploy --only functions
```

Or using the npm script from `functions/`:

```bash
cd functions
npm run deploy
```

### All-in-one

```bash
cd functions && npm install && npm run build && cd .. && firebase deploy --only functions
```

## Function Details

| Property | Value |
|---|---|
| Name | `analyzeReceipt` |
| Type | Callable (`onCall`) |
| Region | `europe-west1` |
| Runtime | Node.js 22 |
| Memory | 512 MiB |
| Timeout | 120 seconds |
| App Check | Disabled (TODO for production) |

## Verifying Deployment

After deploy, verify in the Firebase Console:
```
https://console.firebase.google.com/project/pleasest-e3424/functions
```

Or via CLI:
```bash
firebase functions:list
```

## Local Testing with Emulator

```bash
cd functions
npm run serve
```

This builds and starts the Firebase emulator for functions only.

## Troubleshooting

### "Permission denied" or auth errors
```bash
firebase login --reauth
```

### Build errors
Check TypeScript compilation:
```bash
cd functions && npx tsc --noEmit
```

### Viewing function logs
```bash
firebase functions:log --only analyzeReceipt
```

Or with tail:
```bash
firebase functions:log --only analyzeReceipt --follow
```

### Outdated firebase-functions SDK warning
```bash
cd functions
npm install --save firebase-functions@latest
```
Note: This may introduce breaking changes — check the [migration guide](https://firebase.google.com/docs/functions/manage-functions) before upgrading.
