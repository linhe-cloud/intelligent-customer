package com.IntelligentCustomer.system.repository.typehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class UuidTypeHandlerTest {

    private final UuidTypeHandler typeHandler = new UuidTypeHandler();

    @Test
    void writesUuidAsCanonicalString() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        UUID id = UUID.randomUUID();

        typeHandler.setNonNullParameter(statement, 1, id, JdbcType.CHAR);

        verify(statement).setString(1, id.toString());
    }

    @Test
    void readsUuidFromCharacterColumn() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        when(resultSet.getString("id")).thenReturn(id.toString());

        assertEquals(id, typeHandler.getNullableResult(resultSet, "id"));
    }

    @Test
    void returnsNullForNullColumn() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("id")).thenReturn(null);

        assertNull(typeHandler.getNullableResult(resultSet, "id"));
    }
}
