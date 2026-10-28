# NT-26: RentalDAO

Requires JDK 17, the SQL Server JDBC jar in `web/WEB-INF/lib`, and the NhaTroSV
schema from `setup.sql`. Keep the original `DBContext` unchanged. Configure its
existing `web/WEB-INF/ConnectDB.properties` resource locally with the keys `url`,
`userID`, and `password`. That file is ignored by Git; never commit credentials.

Run from the project directory in PowerShell:

```powershell
$classes = Join-Path $env:TEMP 'nhatro-nt26-check/tomcat/webapps/nt26/WEB-INF/classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
Copy-Item web/WEB-INF/ConnectDB.properties (Join-Path (Split-Path $classes) 'ConnectDB.properties')
javac --release 17 -encoding UTF-8 -Xlint:all -d $classes src/java/model/RentalRequest.java src/java/model/Contract.java src/java/dal/DBContext.java src/java/dal/RentalDAO.java src/java/dal/RentalDAODemo.java test/dal/RentalTestDAO.java test/dal/RentalDAOIntegrationTest.java
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
Select-String -LiteralPath src/java/dal/RentalDAO.java -SimpleMatch '" +'
```

Run `RentalDAODemo.main(new String[0])` and
`RentalDAOIntegrationTest.main(new String[0])` from a temporary test servlet in
that Tomcat web application. Put the JDBC jar in the test application's
`WEB-INF/lib` and the local configuration in `WEB-INF/ConnectDB.properties`.
The original DBContext resolves `../ConnectDB.properties` through Tomcat's web
application class loader; a plain `java -cp` invocation does not resolve that
legacy resource path. Do not deploy the integration-test endpoint to production.

`RentalDAODemo` is read-only. It prints the pending requests or explicitly reports
an empty result. The integration check needs a seeded PENDING request and a
separate tenant without a PENDING request. It inserts its own request, checks
Unicode binding, nullable decision fields, filtering and missing IDs, then tests
contract insertion and atomic rollback, including a UNIQUE violation. Its ENDED
test contract avoids the sample tenants' existing ACTIVE contracts. All contract
writes are rolled back; the test request is deleted in `finally`.
All fixture queries and cleanup SQL live in `test/dal/RentalTestDAO.java`, which
extends the original DBContext; the integration-test class contains assertions
and transaction orchestration only.

`RentalDAO extends DBContext` and uses the inherited `connection`. Keep it open
for the DAO's lifetime so repeated queries work; statements and result sets use
try-with-resources. Use `try (RentalDAO dao = new RentalDAO()) { ... }` to close
that inherited connection when done.

The two INSERT methods return generated IDs. `findById` returns `null` for a
missing request. `insertContract` uses the caller's connection without changing
auto-commit, committing, rolling back or closing it; SQL exceptions propagate to
the service. Set the contract's status explicitly, for example `ACTIVE` in UC09.

Validation on 2026-10-27 with the original DBContext under Tomcat 10.1:
compilation succeeded, the demo returned 3 PENDING
requests, all five integration-check groups passed, and the SQL concatenation
search returned no matches.

After applying AGENTS.md, the five integration-check groups still pass with
fixture SQL isolated in RentalTestDAO.
