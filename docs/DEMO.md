# Demo script (about 10 minutes)

Covers: candidate creation, credential handover, first login and password change, training assignment,
progress, cohort summary, and the security rejections (duplicate email, cross-candidate access,
expired credentials, coordinator reset).

## Before you start

1. Backend running (`mvn spring-boot:run` in `backend`), frontend running (`npm run dev` in `frontend`).
2. Optional clean slate: in MySQL Workbench run
   `DROP DATABASE learning_tracker; CREATE DATABASE learning_tracker CHARACTER SET utf8mb4;`
   then **restart the backend** (it recreates the tables, the seed data and the coordinator).
3. Two browser windows: a normal one for the **coordinator** and a **private/incognito** one for the **candidate**
   (they must not share cookies).
4. Open `http://localhost:5173` in both. Keep this file open for the PowerShell and SQL snippets.

## 1. The product (30 s)

Landing page. Say what it is: coordinators assign and track training, candidates update their own progress.
Click a status in the sample board to show the totals following. Point out: *no public sign-up; the coordinator
creates every account.* Click **Sign in**.

## 2. Coordinator: seeded data and summary (1 min)

Sign in with the coordinator email and password from `.env`.
- **Dashboard:** summary tiles (learners, assignments, in progress, completed), the progress split, learners with
  progress bars, activities. These numbers come from the saved assignments.
- **Learners:** 8 seeded learners in two cohorts. **Activities:** 10 seeded activities.

## 3. Create and edit an activity, with validation (1 min)

**Activities → Add activity.**
1. Click **Save activity** empty: inline messages for title, topic and hours.
2. Enter hours `0`: *"Planned hours must be greater than zero."*
3. Save *Accessible Interfaces / Frontend / 4*: toast "Activity saved", the list updates.
4. **Edit** it and change the hours to 5.
5. Optional: **Delete** it. A confirmation explains that it is also removed from any learner it was assigned to.

## 4. Create a candidate and hand over the credentials (1 min)

**Learners → Add candidate:** `Demo Candidate`, `demo.candidate@example.test`, `Frontend cohort`.
- The one-time credentials panel appears with the **"Shown only once"** warning and the copy button.
- Click **Copy credentials**, paste somewhere private (this is the "privately hand over" step), then **Done**.
- Say: the password is random, stored only as a BCrypt hash, valid 24 hours, and cannot be shown again.

**Security rejection 1: duplicate email.** Add a candidate again with `DEMO.Candidate@Example.test`
(different capitals). The form shows *"A candidate with this email already exists."* inline: the check ignores case.

## 5. Assign training (1 min)

Open **Demo Candidate** (Learners → View). In **Assign an activity** pick *React Basics* and assign it, then
assign *REST APIs*. Toasts confirm; the list shows both as **Not Started**. Already-assigned activities
disappear from the dropdown; the server also rejects duplicates (shown in section 8).

## 6. Candidate: first login and password change (2 min)

In the **private window**: **Sign in** with the copied credentials.
- You are sent to **Choose a new password**, not to the training page.
- Try typing `/my-training` in the address bar: you are sent back. The server also refuses it (403) until the password is changed.
- Fill the form: a short password and a mismatch are rejected inline; the checklist ticks off as you type.
  Use the show/hide toggle. Submit a valid new password.
- You land on **My training** and see only your two activities.

## 7. Update progress and see the summary (1 min)

In the private window set *React Basics* to **In Progress**, then **Completed** ("Progress saved").
In the coordinator window open **Dashboard** and click **Refresh**: completed has gone up, and the learner's
progress bar moved. Reload the page (F5): the numbers are the same, because they come from the database.

## 8. Security rejections (2 min)

### PowerShell helper (coordinator window stays untouched)

```powershell
$base = "http://localhost:8080"
function New-Login($email, $password) {
  $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
  try { Invoke-WebRequest "$base/api/v1/auth/me" -WebSession $s | Out-Null } catch {}
  $t = ($s.Cookies.GetCookies($base) | Where-Object Name -eq 'XSRF-TOKEN').Value
  Invoke-RestMethod -Method Post "$base/api/v1/auth/login" -WebSession $s -Headers @{ 'X-XSRF-TOKEN' = $t } `
    -ContentType 'application/json' -Body (@{ email = $email; password = $password } | ConvertTo-Json) | Out-Null
  return @{ Session = $s; Token = $t }
}
function Try-Call($method, $path, $login, $body = $null) {
  $p = @{ Method = $method; Uri = "$base$path"; WebSession = $login.Session; Headers = @{ 'X-XSRF-TOKEN' = $login.Token } }
  if ($body) { $p.ContentType = 'application/json'; $p.Body = $body }
  try { Invoke-RestMethod @p } catch { "$($_.Exception.Response.StatusCode.value__)  $($_.ErrorDetails.Message)" }
}
```

### 8a. Cross-candidate access and candidate limits

You need a second candidate with a password of their own. Easiest: in the coordinator window reset
Priya's password (Learners → Priya → Reset password), copy the credentials, then in PowerShell:

```powershell
# find assignment ids and who owns them (MySQL Workbench):
#   SELECT a.id, l.display_name FROM assignments a JOIN learners l ON l.id = a.learner_id;

$demo = New-Login "demo.candidate@example.test" "<the NEW password you chose in section 6>"

Try-Call Get   "/api/v1/learners" $demo                                   # 403: a candidate cannot list learners
Try-Call Post  "/api/v1/learners" $demo '{"name":"X","email":"x@example.test","cohort":"C"}'   # 403: cannot create accounts
Try-Call Get   "/api/v1/learners/2/assignments" $demo                     # 403: cannot read another learner's assignments
Try-Call Patch "/api/v1/assignments/<id owned by Priya>/status" $demo '{"status":"Completed"}'  # 403: not your assignment
Try-Call Get   "/api/v1/me/assignments?learnerId=2" $demo                 # 200 but only YOUR training: identity comes from the session
```

Say: ownership is checked on the server by the signed-in account, never by an id sent in the request.

### 8b. Duplicate assignment

```powershell
$coord = New-Login "<coordinator email>" "<coordinator password>"
Try-Call Post "/api/v1/assignments" $coord '{"learnerId":<Demo Candidate id>,"activityId":1}'   # 409 "Assignment already exists."
```

### 8c. Logged-out session

In the private window click **Log out**, then press the browser Back button or open `/my-training`:
you are sent to the login page. In PowerShell, calling with no login gives 401:
`Invoke-RestMethod "$base/api/v1/cohort-summary"`.

### 8d. Expired temporary credentials, then coordinator reset

1. As coordinator add `expired.demo@example.test` (copy the credentials but do not use them).
2. Expire them (MySQL Workbench):
   ```sql
   USE learning_tracker;
   UPDATE accounts SET temp_password_expires_at = UTC_TIMESTAMP() - INTERVAL 2 DAY
   WHERE email = 'expired.demo@example.test';
   ```
3. In the private window try to sign in with those credentials: *"Invalid email or password."* It is the same
   generic message as for a wrong password or an unknown email, so it reveals nothing about the account.
4. Coordinator: **Learners → Expired Demo → Reset password**. New one-time credentials appear.
5. Sign in with the new credentials in the private window: it works and asks for a new password.

### 8e. Reset ends live sessions

1. In the private window stay signed in as a candidate on **My training**.
2. In the coordinator window open that candidate and click **Reset password**.
3. In the private window change any status: the app returns to the login page with *"Your session has ended."*

## 9. Explain the data model and the stack (1 min)

Open `README.md` → *Data model* and *Why this stack*. Key points: four tables; a candidate is a learner linked to
one account; UNIQUE(learner, activity) and `planned_hours > 0` are enforced in both code and database;
Spring Security does hashing, sessions and CSRF; React talks to the API through a same-origin proxy.

## 10. Show the tests (30 s)

```bash
cd backend  && mvn test
cd frontend && npm test
```
Mention the full-workflow test (`WorkflowTest`) and the access-control tests.
