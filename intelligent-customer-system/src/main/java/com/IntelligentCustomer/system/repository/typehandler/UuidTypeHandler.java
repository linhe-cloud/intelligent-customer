package com.IntelligentCustomer.system.repository.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 自定义UUID类型处理器，用于在MyBatis中处理UUID类型与数据库类型之间的转换
 * 该处理器支持将UUID类型映射为数据库的CHAR或VARCHAR类型
 */
@MappedTypes(UUID.class)  // 指定该类型处理器处理的Java类型为UUID
@MappedJdbcTypes(
        value = {JdbcType.CHAR, JdbcType.VARCHAR},  // 指定处理的JDBC数据类型为CHAR和VARCHAR
        includeNullJdbcType = true  // 包含对NULL JDBC类型的处理
)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    /**
     * 将UUID参数设置到PreparedStatement中
     * @param ps PreparedStatement对象
     * @param index 参数索引位置
     * @param parameter UUID类型的参数值
     * @param jdbcType JDBC数据类型
     * @throws SQLException 如果发生数据库访问错误
     */
    @Override
    public void setNonNullParameter(
            PreparedStatement ps,
            int index,
            UUID parameter,
            JdbcType jdbcType
    ) throws SQLException {
        ps.setString(index, parameter.toString());
    }

    /**
     * 从ResultSet中根据列名获取UUID值
     * @param rs ResultSet 对象
     * @param columnName 列名
     * @return UUID值，如果为空则返回null
     * @throws SQLException 如果发生数据库访问错误
     */
    @Override
    public UUID getNullableResult(
            ResultSet rs,
            String columnName
    ) throws SQLException {
        return toUuid(rs.getString(columnName));
    }

    /**
     * 从ResultSet中根据列索引获取UUID值
     * @param rs ResultSet 对象
     * @param columnIndex 列索引
     * @return UUID值，如果为空则返回null
     * @throws SQLException 如果发生数据库访问错误
     */
    @Override
    public UUID getNullableResult(
            ResultSet rs,
            int columnIndex
    ) throws SQLException {
        return toUuid(rs.getString(columnIndex));
    }

    /**
     * 从CallableStatement中根据列索引获取UUID值
     * @param cs CallableStatement对象
     * @param columnIndex 列索引
     * @return UUID值，如果为空则返回null
     * @throws SQLException 如果发生数据库访问错误
     */
    @Override
    public UUID getNullableResult(
            CallableStatement cs,
            int columnIndex
    ) throws SQLException {
        return toUuid(cs.getString(columnIndex));
    }

    /**
     * 将字符串转换为UUID对象
     * @param value 字符串值
     * @return UUID对象，如果输入为null或空字符串则返回null
     */
    private UUID toUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return UUID.fromString(value);
    }
}