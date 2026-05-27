package cn.wubo.sql.forge;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * JUnit5 execution condition that skips Calcite tests when MySQL/PostgreSQL are unavailable.
 * Usage: @ExtendWith(CalciteCondition.class) on test class.
 */
public class CalciteCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("MySQL and PostgreSQL are available");
    private static final ConditionEvaluationResult DISABLED =
            ConditionEvaluationResult.disabled("MySQL and/or PostgreSQL not available at localhost. Calcite tests require both databases running.");

    private static volatile Boolean available;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (available == null) {
            synchronized (CalciteCondition.class) {
                if (available == null) {
                    available = checkAvailability();
                }
            }
        }
        return available ? ENABLED : DISABLED;
    }

    private static boolean checkAvailability() {
        return isPortOpen("localhost", 3306) && isPortOpen("localhost", 5432);
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
