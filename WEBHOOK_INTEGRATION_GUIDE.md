# GitHub Webhook Integration Guide

## Overview

This implementation provides a complete GitHub webhook integration for automatically processing Pull Request events. When a PR is opened, synchronized, closed, or reopened on GitHub, a webhook event is sent to our backend, which automatically creates or updates the PR in our database.

## Architecture

### Components

1. **WebhookController** - REST endpoint receiving webhook events
2. **WebhookService** - Business logic for processing events and signature validation
3. **WebhookEvent Entity** - Stores webhook event history
4. **GithubWebhookPayload DTO** - Maps GitHub webhook JSON payload

### Flow

```
GitHub PR Event → Webhook Endpoint → Signature Validation → Parse Payload → Process Event → Save to Database
```

## Features

✅ **Signature Validation** - HMAC-SHA256 signature verification  
✅ **PR Opened Event** - Auto-create PR in database  
✅ **PR Synchronize Event** - Update PR when new commits pushed  
✅ **PR Closed Event** - Update PR state (closed/merged)  
✅ **PR Reopened Event** - Reopen closed PRs  
✅ **Event History** - All webhook events logged in database  
✅ **Error Handling** - Failed events tracked with error messages  
✅ **Idempotent** - Safe to receive duplicate events  

## Configuration

### 1. Set Webhook Secret

The webhook secret is used to verify that webhook events actually come from GitHub.

**Generate a secret:**
```bash
# Linux/Mac
openssl rand -hex 32

# Or use any strong password generator
```

**Set environment variable:**
```bash
# Windows PowerShell
$env:GITHUB_WEBHOOK_SECRET = "your_generated_secret_here"

# Linux/Mac
export GITHUB_WEBHOOK_SECRET=your_generated_secret_here
```

**Or update application.yml:**
```yaml
app:
  github:
    webhook:
      secret: your_generated_secret_here
```

### 2. Configure GitHub Webhook

**For Production (with public server):**

1. Go to your GitHub repository
2. Navigate to **Settings** → **Webhooks** → **Add webhook**
3. Configure:
   - **Payload URL**: `https://your-server.com/api/webhooks/github`
   - **Content type**: `application/json`
   - **Secret**: Your webhook secret (same as configured above)
   - **Events**: Select "Let me select individual events"
     - ✓ Pull requests
   - **Active**: ✓ Checked
4. Click **Add webhook**

**For Local Testing (with ngrok):**

1. Install ngrok: https://ngrok.com/download
2. Start your Spring Boot application (port 8080)
3. Run ngrok:
   ```bash
   ngrok http 8080
   ```
4. Copy the HTTPS URL (e.g., `https://abc123.ngrok.io`)
5. In GitHub webhook settings:
   - **Payload URL**: `https://abc123.ngrok.io/api/webhooks/github`
   - Configure other settings as above

## API Endpoints

### Webhook Endpoint

```http
POST /api/webhooks/github
Headers:
  X-GitHub-Event: pull_request
  X-Hub-Signature-256: sha256=...
  X-GitHub-Delivery: uuid
Body: GitHub webhook payload (JSON)
```

**Response (Success):**
```json
{
  "message": "Webhook received successfully",
  "event": "pull_request",
  "action": "opened",
  "repository": "owner/repo",
  "prNumber": "1"
}
```

**Response (Invalid Signature):**
```json
{
  "error": "Invalid signature"
}
```

### Test Endpoint

```http
GET /api/webhooks/github
```

**Response:**
```json
{
  "message": "GitHub webhook endpoint is active",
  "status": "ready"
}
```

### Health Check

```http
GET /api/webhooks/health
```

**Response:**
```json
{
  "status": "healthy",
  "service": "webhook"
}
```

## Webhook Events

### 1. Pull Request Opened

**Trigger:** New PR created on GitHub

**Action:**
- Creates new `PullRequest` entity in database
- Sets `analysisStatus` to "pending"
- Stores PR metadata (title, description, branches, etc.)

**Webhook Action:** `opened`

### 2. Pull Request Synchronize

**Trigger:** New commits pushed to PR branch

**Action:**
- Updates `headSha` with new commit SHA
- Resets `analysisStatus` to "pending" (code changed)
- Updates `updatedAt` and `lastSyncedAt` timestamps

**Webhook Action:** `synchronize`

### 3. Pull Request Closed

**Trigger:** PR closed or merged

**Action:**
- Updates `state` to "closed" or "merged"
- Sets `isMerged` flag
- Records `closedAt` and `mergedAt` timestamps

**Webhook Action:** `closed`

### 4. Pull Request Reopened

**Trigger:** Closed PR reopened

**Action:**
- Updates `state` back to "open"
- Updates `updatedAt` timestamp

**Webhook Action:** `reopened`

## Event Processing

### Status Flow

```
received → processing → processed/failed
```

**Status Values:**
- `received` - Webhook received, queued for processing
- `processing` - Currently being processed
- `processed` - Successfully processed
- `failed` - Processing failed (check `errorMessage`)

### Error Handling

If processing fails:
- Event status set to "failed"
- Error message stored in `errorMessage` field
- Event saved to database for debugging

**Common Errors:**
- Repository not found in database
- Pull request not found (for update events)
- Invalid payload format
- Database connection issues

## Database Schema

### webhook_events Collection

```json
{
  "_id": "ObjectId",
  "eventType": "pull_request",
  "action": "opened",
  "repositoryFullName": "owner/repo",
  "repositoryId": 123456789,
  "pullRequestNumber": 1,
  "pullRequestId": 987654321,
  "payload": "{...full JSON payload...}",
  "status": "processed",
  "errorMessage": null,
  "sender": "username",
  "receivedAt": "2026-05-28T10:00:00",
  "processedAt": "2026-05-28T10:00:01",
  "createdAt": "2026-05-28T10:00:00"
}
```

### Indexes

- `eventType` - For filtering by event type
- `repositoryFullName` - For repository-specific queries
- `repositoryId` - Fast lookup by repo ID
- `pullRequestNumber` - Find events for specific PR
- `status` - Filter by processing status
- `createdAt` - Time-based queries

## Security

### Signature Validation

GitHub signs all webhook payloads with HMAC-SHA256 using your webhook secret.

**Validation Process:**
1. Receive payload and `X-Hub-Signature-256` header
2. Calculate HMAC-SHA256 of payload using webhook secret
3. Compare calculated signature with received signature
4. Reject if signatures don't match

**Implementation:**
```java
private String calculateHmacSha256(String data, String key) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec = new SecretKeySpec(
        key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKeySpec);
    byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);
}
```

### Security Best Practices

1. **Always use HTTPS** for webhook endpoint in production
2. **Keep webhook secret confidential** - use environment variables
3. **Validate signatures** - never disable in production
4. **Log security events** - failed signature validations
5. **Rate limiting** - prevent webhook abuse (can be added)

## Testing

### Local Testing

Use the provided `test-webhook-apis.http` file:

```http
POST http://localhost:8080/api/webhooks/github
Content-Type: application/json
X-GitHub-Event: pull_request

{
  "action": "opened",
  "pull_request": { ... },
  "repository": { ... }
}
```

### Verifying Events

**Check webhook events:**
```bash
# MongoDB
db.webhook_events.find().sort({createdAt: -1}).limit(10)
```

**Check created PRs:**
```bash
# MongoDB
db.pull_requests.find({prNumber: 1})
```

### Testing Workflow

1. **Setup**: Create a repository in database first
   ```http
   POST /api/repositories/sync/github
   ```

2. **Send PR Opened Event**: Simulate webhook
   ```http
   POST /api/webhooks/github
   ```

3. **Verify**: Check PR was created
   ```http
   GET /api/pull-requests
   ```

4. **Send Synchronize**: Test update
5. **Send Closed**: Test state change
6. **Check Database**: Verify all events logged

## Troubleshooting

### Webhook not receiving events

- **Check GitHub webhook settings**: Verify URL is correct
- **Check Recent Deliveries**: GitHub shows delivery attempts
- **Use ngrok for local testing**: Expose localhost to internet
- **Check firewall**: Ensure port is accessible

### Signature validation failing

- **Verify secret matches**: Same in GitHub and application
- **Check payload**: Ensure raw body used for signature
- **Environment variable**: Confirm GITHUB_WEBHOOK_SECRET is set

### Events not processing

- **Repository not found**: Add repository to database first
- **Check logs**: Look for error messages
- **Check webhook_events**: Review status and errorMessage
- **Database connection**: Verify MongoDB is running

### PR not created

1. Verify repository exists: `db.repositories.find({fullName: "owner/repo"})`
2. Check webhook event: `db.webhook_events.find({status: "failed"})`
3. Review error message in webhook event
4. Ensure repository fullName matches exactly

## Best Practices

1. **Always sync repository first** before expecting webhook PRs
2. **Monitor webhook_events** for failed processing
3. **Use ngrok** for local development and testing
4. **Set strong webhook secret** (32+ random characters)
5. **Enable HTTPS** in production (required by GitHub)
6. **Log webhook events** for audit trail
7. **Handle duplicates gracefully** (idempotent operations)

## Integration with Analysis

The webhook integration sets up PRs for automatic analysis:

1. PR opened → Created with `analysisStatus: "pending"`
2. Analysis service picks up pending PRs
3. Runs AI analysis on code changes
4. Updates PR with analysis results

This enables **fully automated code review** workflow!

## Future Enhancements

- [ ] Async processing with message queue (RabbitMQ/Kafka)
- [ ] Webhook retry mechanism for failed events
- [ ] Support for more GitHub events (push, issues, comments)
- [ ] Webhook event replay functionality
- [ ] Rate limiting to prevent abuse
- [ ] Webhook delivery statistics dashboard
- [ ] Auto-trigger analysis on PR opened
