package liquibase.database.core;



import liquibase.database.DatabaseConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;

/**
 * 人大金仓 (KingbaseES) Liquibase 适配器
 * 通过 SPI 机制自动加载，无需在 Spring 配置类中手动注册
 */
public class KingbaseESDatabase extends PostgresDatabase {

    /**
     * 提高优先级，确保 Liquibase 优先使用这个类而不是默认的实现
     * 默认优先级通常是 1-10，我们要比它高
     */
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE + 5;
    }

    /**
     * 核心判断逻辑：当数据库产品名为 "KingbaseES" 时，认领该连接
     */
    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return "KingbaseES".equalsIgnoreCase(conn.getDatabaseProductName());
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "KingbaseES";
    }

    // 依然保持短名称为 postgresql，复用 PG 的 changeLog
    @Override
    public String getShortName() {
        return "postgres";
    }
}