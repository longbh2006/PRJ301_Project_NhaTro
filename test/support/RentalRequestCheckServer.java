package support;

import java.lang.reflect.InvocationTargetException;
import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.startup.Tomcat;

/** Server cục bộ chỉ để kiểm thử; không đóng gói thư mục test vào ứng dụng. */
public class RentalRequestCheckServer {
    public static void main(String[] args) throws Exception {
        Tomcat server = new Tomcat();
        server.setBaseDir(args[1]);
        server.setPort(18094);
        server.getConnector().setProperty("address", "127.0.0.1");
        Context context = server.addWebapp("/nt34", args[0]);
        try {
            server.start();
            ClassLoader loader = context.getLoader().getClassLoader();
            Class.forName("service.RentalServiceIntegrationTest", true, loader)
                    .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            Class<?> fixtureClass = Class.forName("dal.RentalRequestTestDAO", true, loader);
            try (AutoCloseable fixtures = (AutoCloseable) fixtureClass.getConstructor().newInstance()) {
                int tenantId = (Integer) fixtureClass.getMethod("createTenant").invoke(fixtures);
                int otherTenantId = (Integer) fixtureClass.getMethod("createTenant").invoke(fixtures);
                try {
                    int roomId = (Integer) fixtureClass.getMethod("findRoom", boolean.class).invoke(fixtures, false);
                    int occupiedRoomId = (Integer) fixtureClass.getMethod("findRoom", boolean.class).invoke(fixtures, true);
                    Class.forName("controller.RentalRequestWebIntegrationTest", true, loader)
                            .getMethod("run", String.class, String.class, String.class, String.class,
                                    int.class, int.class, int.class)
                            .invoke(null, "http://127.0.0.1:18094/nt34", cookie(context, tenantId, "TENANT"),
                                    cookie(context, otherTenantId, "TENANT"), cookie(context, tenantId, "MANAGER"),
                                    otherTenantId, roomId, occupiedRoomId);
                } finally {
                    fixtureClass.getMethod("deleteTenant", int.class).invoke(fixtures, tenantId);
                    fixtureClass.getMethod("deleteTenant", int.class).invoke(fixtures, otherTenantId);
                }
            }
            System.out.println("PASS: all NT-34 checks completed and fixtures removed");
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("NT-34 integration check failed", ex.getCause());
        } finally {
            server.stop();
            server.destroy();
        }
    }

    private static String cookie(Context context, int accountId, String role) {
        Session session = context.getManager().createSession(null);
        session.getSession().setAttribute("accountId", accountId);
        session.getSession().setAttribute("role", role);
        return "JSESSIONID=" + session.getId();
    }
}
