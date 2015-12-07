package com.soprasteria.notificationemail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.log4j.Logger;

/**
 * Class that represents database object.
 *
 * @author sgacka
 */
public class Database {

    private final String user;
    private final String password;
    private final String host;
    private final int port;
    private final String sid;
    private OracleConnection connection;
    private static final Logger logger = Logger.getLogger(Database.class);

    /**
     * Database constructor.
     *
     * @param user Database user name
     * @param password Database user password
     * @param host Database host name/IP
     * @param port Listening port
     * @param sid Database SID (name)
     */
    public Database(String user, String password, String host, int port, String sid) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;
        this.sid = sid;
    }

    /**
     * Opens connection to database.
     */
    public void connect() {
        try {
            OracleDataSource ods = new OracleDataSource();

            ods.setDriverType("thin");
            ods.setUser(user);
            ods.setPassword(password);
            ods.setServerName(host);
            ods.setPortNumber(port);
            ods.setDatabaseName(sid); // sid

            logger.info("Connecting to database... (User: " + Configuration.db_user + ", Host: " + Configuration.db_host + ", Port: " + Configuration.db_port + ")");

            connection = (OracleConnection) (ods.getConnection());
            connection.setDefaultExecuteBatch(100);
        } catch (Throwable e) {
            logger.fatal("Unable to connect to database", e);

            Configuration.stop = Calendar.getInstance().getTime();
            logger.info("Execution time: " + (double) (Configuration.stop.getTime() - Configuration.start.getTime()) / 1000 + " seconds");
            logger.info("ERROR. Application ended with error.");

            System.exit(1);
        } finally {
            logger.info("Connection to database has been opened");
        }
    }

    /**
     * Closes connection to database.
     */
    public void disconnect() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (Throwable e) {
            logger.error("Unable to close connection to database", e);
        } finally {
            logger.info("Connection to database has been closed");
        }
    }

    /**
     * Commits changes to database.
     */
    public void commit() {
        try {
            connection.commit();
        } catch (Throwable e) {
            logger.error("Unable to commit changes to database", e);
        }
    }

    /**
     * Gets list of eventout records.
     *
     * @return List of eventout records
     * @throws Exception
     */
    public ArrayList<EventOutRecord> getEventOutRecords() throws Exception {
        Company companyReocrds = new Company(this);
        companyReocrds.getCompanies();

        ArrayList<EventOutRecord> records = new ArrayList<EventOutRecord>();

        String query = "SELECT evfields, CAST(FROM_TZ(CAST(evtime AS TIMESTAMP), 'utc') AT TIME ZONE sessiontimezone AS DATE), evsysseq FROM eventoutm1 WHERE evtype = 'email' and (evstatus is null or evstatus not in ('error', 'warning')) and evtime IS NOT NULL and evsysseq IS NOT NULL";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            records.add(new EventOutRecord(resultSet.getClob(1), resultSet.getTimestamp(2).getTime(), resultSet.getString(3), companyReocrds, this));
        }

        logger.info("Total eventout records found: " + records.size());

        return records;
    }

    public ArrayList<CompanyRecord> getCompanies() throws Exception {
        ArrayList<CompanyRecord> records = new ArrayList<CompanyRecord>();

        String query = "SELECT company, gsc_sender_name, gsc_sender_mail FROM companym1 ORDER BY company";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            records.add(new CompanyRecord(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)));
            logger.trace(resultSet.getString(1) + " -- " + resultSet.getString(2) + " -- " + resultSet.getString(3));
        }

        logger.info("Total company records found: " + records.size());

        return records;
    }

    /**
     * Removes eventout record based on unique evsysseq value.
     *
     * @param evSysSeq evsysseq key value
     * @throws Exception
     */
    public synchronized void removeRecordFromEventOut(String evSysSeq) throws Exception {
        PreparedStatement pStatement = connection.prepareStatement("DELETE FROM eventoutm1 WHERE evsysseq = ? AND evtype = 'email'");
        pStatement.setString(1, evSysSeq);
        pStatement.executeUpdate();

        pStatement.close();

        logger.info("Record removed: evsysseq = " + evSysSeq);
    }

    /**
     * Updates eventout record based on unique evsysseq value to mark it as
     * invalid.
     *
     * @param evSysSeq evsysseq key value
     * @param status status of processing
     * @throws Exception
     */
    public synchronized void markRecord(String evSysSeq, String status) throws Exception {
        PreparedStatement pStatement = connection.prepareStatement("UPDATE eventoutm1 SET evstatus = ? WHERE evsysseq = ? AND evtype = 'email'");
        pStatement.setString(1, status);
        pStatement.setString(2, evSysSeq);
        pStatement.executeUpdate();

        pStatement.close();

        logger.info("Record marked as error: evsysseq = " + evSysSeq);
    }
}
