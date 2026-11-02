# NT-26: RentalDAO

Requires JDK 17, the SQL Server JDBC jar in `web/WEB-INF/lib`, and the NhaTroSV
schema from `setup.sql`. Keep the original `DBContext` unchanged. Configure its
existing `web/WEB-INF/ConnectDB.properties` resource locally with the keys `url`,
`userID`, and `password`. That file is ignored by Git; never commit credentials.

Build and deploy the actual project following [the local run guide](run-local.md).
Inspect pending requests through `/manager/requests` using an authenticated
manager session. Check request and contract persistence through the real rental
workflow described in [NT-36](NT-36-validation.md).

Follow AGENTS.md: check the real application's UI or HTTP endpoints; do not add
demo programs, temporary test servlets or separate test applications. The legacy
DBContext resolves `../ConnectDB.properties` through Tomcat's web application
class loader, so check database access in the deployed application.

`RentalDAO extends DBContext` and uses the inherited `connection`. Keep it open
for the DAO's lifetime so repeated queries work; statements and result sets use
try-with-resources. Use `try (RentalDAO dao = new RentalDAO()) { ... }` to close
that inherited connection when done.

The two INSERT methods return generated IDs. `findById` returns `null` for a
missing request. `insertContract` uses the caller's connection without changing
auto-commit, committing, rolling back or closing it; SQL exceptions propagate to
the service. Set the contract's status explicitly, for example `ACTIVE` in UC09.

Validation on 2026-10-27 with the original DBContext under Tomcat 10.1:
compilation succeeded, the pending query returned 3 PENDING
requests, all five existing integration-check groups passed, and the SQL concatenation
search returned no matches.

After applying AGENTS.md, the five integration-check groups still pass with
fixture SQL isolated in RentalTestDAO.
