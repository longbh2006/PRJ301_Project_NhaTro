# NT-34 — Người thuê gửi yêu cầu thuê phòng

Implements [NT-34](https://cuongthai.com/work/prj301-nha-tro-sinh-vien/NT/issue/34).

- `GET /rooms/detail?roomId=...`: room details and the tenant's **Gửi yêu cầu thuê phòng này** link.
- `GET /tenant/requests?roomId=...`: rental form.
- `POST /tenant/requests`: validate, create PENDING, redirect to `/tenant/requests`.
- `GET /tenant/requests`: the signed-in tenant's requests and a one-time success message.

Servlet declarations and URL mappings for this branch are centralized in
`web/WEB-INF/web.xml`; controllers do not use `@WebServlet` annotations.

## Wireframe alignment

Uses P02 and P05 from `RoomRent_Wireframe_14_Trang_Ghi_Ro_URL.drawio`:

- P02 has the public header without a sidebar, a large room-image area with
  thumbnail placeholders, room information, a status badge, the blue request
  button and the back link. Room area and image URL come from the database.
- P05 keeps the tenant sidebar, a status/search filter, five columns (request
  code, room, sent date, status, actions), colored status badges and a details
  panel. `Xem` only opens requests belonging to the session's tenant. Filters
  support real room codes and displayed `YC` request codes.
- The request form follows the same colors, borders and spacing, with a room
  summary beside the rental fields; the reference has no separate form page.
- Room location, a quoted deposit and a multi-image gallery are not stored in
  the current room model. The screen shows actual room type/capacity and labels
  the deposit as determined by the contract, rather than inventing sample data.
- Cancellation, contract actions and authentication remain with their own
  tickets. The wireframe's page numbers, URLs and flow annotations are design
  notes and are not rendered as application controls.

The login controller must set `session.accountId` as an `Integer` and
`session.role` as `TENANT`. Tenant IDs in submitted forms are ignored. Login is
not implemented on the current base branch; the HTTP checks create sessions
through Tomcat's test harness. The separate room-details controller leaves the
room-list controller on the NT-32 branch available for integration. Room-list
links should use `/rooms/detail?roomId=<roomId>`.

RentalService enforces the room's ACTIVE status, no ACTIVE room contract, room
capacity, a move-in date from today, 1–24 months, a positive party size, a note
of at most 500 characters (the actual database column size), no ACTIVE tenant
contract and no existing PENDING request. A SERIALIZABLE transaction and an
update lock on the tenant serialize duplicate submissions. RentalDAO owns and
closes its inherited connection; RentalService commits or rolls back before the
DAO closes. DBContext and the filter mappings were not changed.

## Checks run on 2026-10-28

JDK 17, Tomcat 10.1 and the existing SQL Server database:

- Java compilation succeeded.
- Both acceptance criteria passed: occupied room rejected with its reason;
  the second sequential request rejected with only one saved row.
- Concurrent submissions also saved exactly one PENDING request.
- Invalid date, duration, party size, capacity, unknown room and long note rejected.
- Existing ACTIVE tenant contract rejected.
- UTF-8 and SQL-like note text round-tripped correctly.
- HTTP checks passed for guest redirects, forbidden roles, protected JSPs,
  room detail, form rendering, invalid input preservation, PRG, one-time flash,
  tenant ownership and both reflected and stored XSS escaping.
- Status/room/request-code filters and request-detail ownership checks passed.
- Test-owned accounts and requests were removed; existing fixtures were not modified.
- Changed files were checked against AGENTS.md; no SQL outside DAO classes,
  SQL concatenation or JSP scriptlets were introduced.

## Reproduce

Run from the repository root in PowerShell. Requires an ACTIVE occupied room
and an ACTIVE vacant room in the sample database. The harness listens only on
`127.0.0.1:18094` and stops after the checks. Keep the `test` directory out of
the deployed application's classes.

```powershell
$ErrorActionPreference = 'Stop'
$tomcatPath = 'C:/Program Files/Apache Software Foundation/Tomcat 10.1'
$jdkPath = Split-Path -Parent (Get-Command javac).Source
$checkPath = Join-Path $env:TEMP ('nhatro-nt34-' + [guid]::NewGuid())
$webPath = Join-Path $checkPath 'web'
$classesPath = Join-Path $webPath 'WEB-INF/classes'
New-Item -ItemType Directory -Path $classesPath -Force | Out-Null
Copy-Item web/WEB-INF/views,web/WEB-INF/tags,web/WEB-INF/lib (Join-Path $webPath 'WEB-INF') -Recurse
Copy-Item web/WEB-INF/web.xml (Join-Path $webPath 'WEB-INF/web.xml')
Copy-Item web/css (Join-Path $webPath 'css') -Recurse

# Use the local configuration already supported by the original DBContext.
$configPath = 'web/WEB-INF/ConnectDB.properties'
if (!(Test-Path $configPath)) { $configPath = 'build/web/WEB-INF/ConnectDB.properties' }
Copy-Item $configPath (Join-Path $webPath 'WEB-INF/ConnectDB.properties')
$driverPath = 'web/WEB-INF/lib/mssql-jdbc-13.2.0.jre11.jar'
if (!(Test-Path $driverPath)) { $driverPath = 'build/web/WEB-INF/lib/mssql-jdbc-13.2.0.jre11.jar' }
Copy-Item $driverPath (Join-Path $webPath 'WEB-INF/lib/mssql-jdbc-13.2.0.jre11.jar') -Force

$sources = @(Get-ChildItem src/java,test -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$jdkPath/javac.exe" --release 17 -encoding UTF-8 -cp "$tomcatPath/lib/*" -d $classesPath @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$runnerPath = Join-Path $checkPath 'runner'
& "$jdkPath/javac.exe" --release 17 -encoding UTF-8 -cp "$tomcatPath/lib/*" -d $runnerPath test/support/RentalRequestCheckServer.java
if ($LASTEXITCODE -ne 0) { throw 'Runner compilation failed' }
& "$jdkPath/java.exe" -cp "$runnerPath;$tomcatPath/lib/*;$tomcatPath/bin/tomcat-juli.jar" support.RentalRequestCheckServer $webPath (Join-Path $checkPath 'tomcat')
if ($LASTEXITCODE -ne 0) { throw 'Integration checks failed' }
```
