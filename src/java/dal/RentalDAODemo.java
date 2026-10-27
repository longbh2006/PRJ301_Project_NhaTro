package dal;

import java.sql.SQLException;
import java.util.List;
import model.RentalRequest;

/** Run after setup.sql and ConnectDB.properties configuration to check the pending query. */
public class RentalDAODemo {

    public static void main(String[] args) throws SQLException {
        try (RentalDAO dao = new RentalDAO()) {
            printPendingRequests(dao.findPendingRequests());
        }
    }

    private static void printPendingRequests(List<RentalRequest> requests) {
        System.out.printf("PENDING requests: %d%n", requests.size());
        for (RentalRequest request : requests) {
            System.out.printf(
                    "requestId=%d, tenantId=%d, roomId=%d, moveInDate=%s, months=%d, numPeople=%d, status=%s%n",
                    request.getRequestId(), request.getTenantId(), request.getRoomId(),
                    request.getMoveInDate(), request.getMonths(), request.getNumPeople(), request.getStatus());
        }
        if (requests.isEmpty()) {
            System.out.println("Query OK. No PENDING requests; add a sample request and run again.");
        }
    }
}
