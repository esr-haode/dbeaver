/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol.model.cache;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolView;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;

import java.sql.SQLException;

/**
 * Cache for Exasol Views
 *
 * @author Karl Griesser
 */
public class ExasolViewCache extends JDBCStructCache<ExasolSchema, ExasolView, ExasolTableColumn> {

    /* rename columns for compatibility to TableBase Object */
    private static final String SQL_VIEWS =
        "select * from ("
            + " SELECT "
            + " VIEW_OWNER AS TABLE_OWNER,"
            + " VIEW_NAME AS TABLE_NAME, "
            + " VIEW_COMMENT AS REMARKS,"
            + " 'VIEW' as TABLE_TYPE,"
            + " VIEW_TEXT FROM EXA_ALL_VIEWS "
            + " WHERE VIEW_SCHEMA = '%s' "
            + " union all "
            + " select "
            + " 'SYS' as TABLE_OWNER, "
            + " object_name as TABLE_NAME, "
            + " object_comment as REMARKS, "
            + " object_type, "
            + " 'N/A for sysobjects' as view_text "
            + " from sys.exa_syscat "
            + " where  "
            + "   SCHEMA_NAME = '%s' "
            + " ) "
            + "order by table_name";
    private static final String SQL_COLS_VIEW = "SELECT c.*,CAST(NULL AS INTEGER) as key_seq FROM  \"$ODBCJDBC\".\"ALL_COLUMNS\"  c WHERE c.table_SCHEM = '%s' AND c.TABLE_name = '%s' order by ORDINAL_POSITION";
    private static final String SQL_COLS_ALL =  "SELECT c.*,CAST(NULL AS INTEGER) as key_seq FROM  \"$ODBCJDBC\".\"ALL_COLUMNS\"  c WHERE c.table_SCHEM = '%s' order by c.TABLE_name,ORDINAL_POSITION";


    public ExasolViewCache() {
        super("TABLE_NAME");

    }

    @SuppressWarnings("rawtypes")
	@Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema) throws SQLException {
        JDBCStatement dbStat = session.createStatement();
        
        String sql = String.format(SQL_VIEWS, ExasolUtils.quoteString(exasolSchema.getName()),ExasolUtils.quoteString(exasolSchema.getName()));
        
        ((JDBCStatementImpl) dbStat).setQueryString(sql);
        return dbStat;
    }

    @Override
    protected ExasolView fetchObject(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @NotNull JDBCResultSet dbResult) throws SQLException,
        DBException {
        return new ExasolView(session.getProgressMonitor(), exasolSchema, dbResult);
    }

    @SuppressWarnings("rawtypes")
	@Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @Nullable ExasolView forView) throws SQLException {
        String sql;
        if (forView != null) {
            sql = String.format(SQL_COLS_VIEW, ExasolUtils.quoteString(exasolSchema.getName()), ExasolUtils.quoteString(forView.getName())) ;
        } else {
            sql = String.format(SQL_COLS_ALL, ExasolUtils.quoteString(exasolSchema.getName()));
        }

        JDBCStatement dbStat = session.createStatement();
        
        ((JDBCStatementImpl) dbStat).setQueryString(sql);

        return dbStat;

    }

    @Override
    protected ExasolTableColumn fetchChild(@NotNull JDBCSession session, @NotNull ExasolSchema exasolSchema, @NotNull ExasolView exasolView, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException {
        return new ExasolTableColumn(session.getProgressMonitor(), exasolView, dbResult);
    }

}
