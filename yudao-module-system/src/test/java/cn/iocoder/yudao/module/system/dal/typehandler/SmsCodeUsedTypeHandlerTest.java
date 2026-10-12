package cn.iocoder.yudao.module.system.dal.typehandler;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SmsCodeUsedTypeHandlerTest {

    private final SmsCodeUsedTypeHandler handler = new SmsCodeUsedTypeHandler();

    @Test
    public void writeUsesSmallintInsteadOfBoolean() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 1, false, JdbcType.SMALLINT);
        handler.setNonNullParameter(statement, 2, true, JdbcType.SMALLINT);
        verify(statement).setShort(1, (short) 0);
        verify(statement).setShort(2, (short) 1);
        verifyNoMoreInteractions(statement);
    }

    @Test
    public void readSmallintAsBoolean() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getShort("used")).thenReturn((short) 0, (short) 1);
        assertFalse(handler.getNullableResult(resultSet, "used"));
        assertTrue(handler.getNullableResult(resultSet, "used"));
        when(resultSet.wasNull()).thenReturn(true);
        assertNull(handler.getNullableResult(resultSet, "used"));
    }
}
