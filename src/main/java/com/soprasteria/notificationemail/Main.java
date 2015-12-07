package com.soprasteria.notificationemail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 * Main class of the application.
 *
 * @author sgacka
 */
public class Main {

    static Logger logger = Logger.getLogger(Main.class);

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        //args = new String[]{"D:\\sgaconfig.cfg", "-R"};

        Configuration.start = Calendar.getInstance().getTime();

        if (args.length > 2 || args.length < 1) {
            printUsage();

            System.exit(0);
        } else {
            if (args.length == 2) {
                if (args[1].toUpperCase().equals("-R")) {
                    Configuration.isReadOnly = Boolean.TRUE;
                } else {
                    printUsage();

                    System.exit(0);
                }
            } else {
                // go standard
            }
        }

        if (Configuration.loadConfigurationFile(args[0])) {
            logger.info("<------------  Starting Notification E-mail 1.0.1  ------------>");
            logger.info("Getting notifications for: " + Configuration.customer_tool);

            processing();
        } else {
            printUsage();

            System.exit(1);
        }
    }

    /**
     * Stars processing eventout table.
     *
     * @throws Exception
     * @throws InterruptedException
     */
    private static void processing() throws Exception, InterruptedException {
        if (Configuration.isReadOnly) {
            logger.info("Process information: eventout is in read-only mode");
        }

        if (Configuration.include_companies == null && Configuration.exclude_companies == null) {
            logger.info("All companies will be treated.");
        } else if (Configuration.include_companies != null && Configuration.exclude_companies == null) {
            logger.info("Companies that will be treated: " + Configuration.include_companies);
        } else if (Configuration.include_companies == null && Configuration.exclude_companies != null) {
            logger.info("Companies that will be ignored: " + Configuration.exclude_companies);
        } else {
            logger.info("Companies that will be treated: " + Configuration.include_companies);
            logger.info("Companies that will be ignored: " + Configuration.exclude_companies);
        }

        Database db = new Database(Configuration.db_user, Configuration.db_password, Configuration.db_host, Configuration.db_port, Configuration.db_sid);
        db.connect();

        EventOut eventOut = new EventOut(db);
        eventOut.getEventOut();

        if (eventOut.getRecordsCount() > 0) {
            logger.info("Starting eventout processing...");
            Date start = Calendar.getInstance().getTime();

            ExecutorService threadExecutor = Executors.newFixedThreadPool(100);
            ArrayList<EventOutRecord> eventRecords = new ArrayList<EventOutRecord>();

            EventOutRecord eor;
            for (int index = 0; index < eventOut.getRecordsCount(); index++) {
                eor = eventOut.getEventOutRecord(index);
                eventRecords.add(eor);
            }

            // only execute() can be here!
            for (int index = 0; index < eventRecords.size(); index++) {
                threadExecutor.execute(eventRecords.get(index));
            }

            threadExecutor.shutdown();
            while (!threadExecutor.isTerminated()) {
                threadExecutor.awaitTermination(1, TimeUnit.SECONDS);
            }

            db.commit();

            Date stop = Calendar.getInstance().getTime();
            logger.info("Eventout processing complete in " + (double) (stop.getTime() - start.getTime()) / 1000 + " seconds");

            logger.info(Configuration.getNotificationsCount());
            db.disconnect();
            Configuration.stop = Calendar.getInstance().getTime();

            logger.info("Execution time: " + (double) (Configuration.stop.getTime() - Configuration.start.getTime()) / 1000 + " seconds");
            logger.info("SUCCESS. Application ended with success.");
        } else {
            logger.info("No records found");
            db.disconnect();
            Configuration.stop = Calendar.getInstance().getTime();

            logger.info("Execution time: " + (double) (Configuration.stop.getTime() - Configuration.start.getTime()) / 1000 + " seconds");
            logger.info("SUCCESS. Application ended with success.");
        }
    }

    /**
     * Prints usage information for this application.
     */
    private static void printUsage() {
        System.out.println("\r\nCorrect usage:\tjava -jar NotificationEmail [CONFIG_PATH] <MODE> <ATTACHMENT>");
        System.out.println("where:\r\nCONFIG_PATH is:\r\n\tpath to the configuration file");
        System.out.println("MODE is:\r\n\t-R\tread only (optional)");
    }
}
