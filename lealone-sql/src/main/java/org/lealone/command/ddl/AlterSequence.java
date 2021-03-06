/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.lealone.command.ddl;

import org.lealone.api.ErrorCode;
import org.lealone.command.CommandInterface;
import org.lealone.dbobject.Right;
import org.lealone.dbobject.Schema;
import org.lealone.dbobject.Sequence;
import org.lealone.dbobject.table.Column;
import org.lealone.dbobject.table.Table;
import org.lealone.engine.Database;
import org.lealone.engine.Session;
import org.lealone.expression.Expression;
import org.lealone.message.DbException;

/**
 * This class represents the statement
 * ALTER SEQUENCE
 */
public class AlterSequence extends SchemaCommand {

    private Table table;
    private Sequence sequence;
    private Expression start;
    private Expression increment;

    public AlterSequence(Session session, Schema schema) {
        super(session, schema);
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean isTransactional() {
        return true;
    }

    public void setColumn(Column column) {
        table = column.getTable();
        sequence = column.getSequence();
        if (sequence == null) {
            throw DbException.get(ErrorCode.SEQUENCE_NOT_FOUND_1, column.getSQL());
        }
    }

    public void setStartWith(Expression start) {
        this.start = start;
    }

    public void setIncrement(Expression increment) {
        this.increment = increment;
    }

    @Override
    public int update() {
        Database db = session.getDatabase();
        if (table != null) {
            session.getUser().checkRight(table, Right.ALL);
        }
        if (start != null) {
            long startValue = start.optimize(session).getValue(session).getLong();
            sequence.setStartValue(startValue);
        }
        if (increment != null) {
            long incrementValue = increment.optimize(session).getValue(session).getLong();
            if (incrementValue == 0) {
                throw DbException.getInvalidValueException("INCREMENT", 0);
            }
            sequence.setIncrement(incrementValue);
        }
        // need to use the system session, so that the update
        // can be committed immediately - not committing it
        // would keep other transactions from using the sequence
        Session sysSession = db.getSystemSession();
        synchronized (sysSession) {
            db.update(sysSession, sequence);
            sysSession.commit(true);
        }
        return 0;
    }

    @Override
    public int getType() {
        return CommandInterface.ALTER_SEQUENCE;
    }

}
