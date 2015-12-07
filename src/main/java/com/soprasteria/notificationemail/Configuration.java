package com.soprasteria.notificationemail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

/**
 * Class that represents application configuration.
 *
 * @author sgacka
 */
public class Configuration {

    // Customer tool for which ticket should be extracted
    public static String customer_tool = null;
    // Included companies list
    public static List<String> include_companies = null;
    // Excluded companies list
    public static List<String> exclude_companies = null;
    // Overrides notification e-mail settings
    public static Address new_sender = null;
    public static Address[] new_recipients = null;
    public static Address[] new_cc = null;
    public static Address[] new_bcc = null;
    // Eventout separator
    public static String separator = null;
    // E-mail validation is on when TRUE
    public static Boolean validate_email = Boolean.FALSE;
    // Database configuration
    public static String db_user = null;
    public static String db_password = null;
    public static String db_host = null;
    public static int db_port = 0;
    public static String db_sid = null;
    // SMTP configuration
    public static String smtp_user = null;
    public static String smtp_password = null;
    public static String smtp_host = null;
    public static int smtp_port = 0;
    public static Boolean smtp_req_auth = null;
    public static Boolean smtp_ssl = Boolean.FALSE;
    // Log path
    private static String logPath = null;
    // Records are not removed TRUE
    public static Boolean isReadOnly = Boolean.FALSE;
    // E-mail encoding
    public static String encoding = null;
    // Execution time variable - start time
    public static Date start;
    // Execution time variable - stop time
    public static Date stop;
    // Notification counter
    private static int matchingNotificationsCount = 0;
    // Properties from config file
    private static Properties properities;
    // Non-breaking space char
    public static final String NBSP = "\u00A0";

    /**
     * Loads database, etc. configuration from file.
     *
     * @param configPath Path to the configuration file
     * @return TRUE if configuration was loaded successfully
     */
    public static Boolean loadConfigurationFile(String configPath) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setProvider(new BouncyCastleProvider());
        encryptor.setAlgorithm("PBEWITHSHA256AND256BITAES-CBC-BC");
        encryptor.setPassword("AS#*O&q5g\\/s/fg~asdf~2SH4JD345*T$[]wee");

        properities = new EncryptableProperties(encryptor);

        try {
            if (new File(configPath).exists()) {
                properities.load(new FileInputStream(configPath));
            } else {
                throw new FileNotFoundException();
            }

            // setting values
            validateBasicInfo();

            // email addresses override
            validateEmailInfo();

            // database
            validateDatabaseInfo();

            // smtp
            validateSmtpInfo();

            // set log path for logger
            validatePaths();

            // set logger properties
            PropertyConfigurator.configure(properities);

            return Boolean.TRUE;
        } catch (FileNotFoundException fnfe) {
            System.out.println("\r\nNotificationEmail - Unable to locate configuration file.\r\nPlease check if the following path is correct: \"" + configPath + "\"\r\n" + fnfe.toString());

            return Boolean.FALSE;
        } catch (IOException ie) {
            System.out.println("\r\nNotificationEmail - Exception while loading configuration file:\r\n" + ie.toString());

            return Boolean.FALSE;
        }
    }

    /**
     * Validates basic configuration parameters.
     */
    private static void validateBasicInfo() {
        // validate customer tool
        customer_tool = properities.getProperty("customer_tool");
        if (customer_tool == null || customer_tool.isEmpty()) {
            System.out.println("Customer tool name was not provided!");
            System.exit(1);
        } else {
            customer_tool = customer_tool.toUpperCase();
        }

        // validate notification Time Zone
        String temp;
        temp = properities.getProperty("include_companies");
        if (temp != null && temp.isEmpty() == false) {
            include_companies = Arrays.asList(temp.toUpperCase().split(","));

            for (int i = 0; i < include_companies.size(); i++) {
                include_companies.set(i, include_companies.get(i).trim());
            }
        }

        // validate notification Time Zone
        temp = properities.getProperty("exclude_companies");
        if (temp != null && temp.isEmpty() == false) {
            exclude_companies = Arrays.asList(temp.toUpperCase().split(","));

            for (int i = 0; i < exclude_companies.size(); i++) {
                exclude_companies.set(i, exclude_companies.get(i).trim());
            }
        }

        // validate e-mail encoding
        encoding = properities.getProperty("email_encoding");
        if (encoding == null || encoding.isEmpty()) {
            encoding = "utf-8";
        }

        // validate eventout separator
        separator = properities.getProperty("eventout_separator");
        if (separator == null || separator.isEmpty()) {
            separator = "^";
        }

        // validate e-mail address validation flag
        Boolean email_val = Boolean.parseBoolean(properities.getProperty("validate_email"));
        if (email_val) {
            validate_email = Boolean.TRUE;
        } else {
            validate_email = Boolean.FALSE;
        }
    }

    /**
     * Validates override email parameters.
     */
    private static void validateEmailInfo() {
        getSenderEmail();
        new_recipients = getOtherEmails("new_recipients");
        new_cc = getOtherEmails("new_cc");
        new_bcc = getOtherEmails("new_bcc");
    }

    /**
     * Validates database configuration parameters.
     */
    private static void validateDatabaseInfo() {
        db_user = properities.getProperty("db_user");
        db_password = properities.getProperty("db_password");
        db_host = properities.getProperty("db_host");
        db_port = getNumber(properities.getProperty("db_port"));
        db_sid = properities.getProperty("db_sid");

        if (db_user == null || db_user.isEmpty()) {
            System.out.println("NotificationEmail - Database user was not provided!");
            System.exit(1);
        }

        if (db_password == null || db_password.isEmpty()) {
            System.out.println("NotificationEmail - Warning! - Database password was not provided");
        }

        if (db_host == null || db_host.isEmpty()) {
            System.out.println("NotificationEmail - Database host was not provided!");
            System.exit(1);
        }

        if (db_port < 1 || db_port > 65535) {
            System.out.println("NotificationEmail - Database port is out of range!");
            System.exit(1);
        }

        if (db_sid == null || db_sid.isEmpty()) {
            System.out.println("NotificationEmail - Database user was not provided!");
            System.exit(1);
        }
    }

    /**
     * Validates SMTP configuration parameters.
     */
    private static void validateSmtpInfo() {
        smtp_user = properities.getProperty("smtp_user");
        smtp_password = properities.getProperty("smtp_password");
        smtp_host = properities.getProperty("smtp_host");
        smtp_port = getNumber(properities.getProperty("smtp_port"));

        Boolean smtp_bool = Boolean.parseBoolean(properities.getProperty("smtp_req_auth"));
        if (smtp_bool) {
            smtp_req_auth = Boolean.TRUE;
        } else {
            smtp_req_auth = Boolean.FALSE;
        }

        smtp_bool = Boolean.parseBoolean(properities.getProperty("smtp_ssl"));
        if (smtp_bool) {
            smtp_ssl = Boolean.TRUE;
        } else {
            smtp_ssl = Boolean.FALSE;
        }

        if (smtp_user == null || smtp_user.isEmpty()) {
            if (smtp_req_auth || smtp_ssl) {
                System.out.println("NotificationEmail - SMTP user was not provided!");
                System.exit(1);
            } else {
                System.out.println("NotificationEmail - Warning! - SMTP user was not provided");
            }
        }

        if (smtp_password == null || smtp_password.isEmpty()) {
            System.out.println("NotificationEmail - Warning! - SMTP password was not provided");
        }

        if (smtp_host == null || smtp_host.isEmpty()) {
            System.out.println("NotificationEmail - SMTP host was not provided!");
            System.exit(1);
        }

        if (smtp_port < 1 || smtp_port > 65535) {
            System.out.println("NotificationEmail - SMTP port is out of range!");
            System.exit(1);
        }
    }

    /**
     * Validates provided paths.
     */
    private static void validatePaths() {
        Boolean isAbsolute;

        // validate log path
        logPath = properities.getProperty("log_path");
        isAbsolute = isDirectory(logPath);

        if (!isAbsolute && (logPath == null || logPath.isEmpty())) {
            System.out.println("NotificationEmail - Log path was not provided!");
            System.exit(1);
        }

        properities.setProperty("log.dir", logPath);
        properities.setProperty("log4j.appender.A2.file", "${log.dir}/" + customer_tool + "_NOTIFICATION_EMAIL.log");
    }

    /**
     * Increases matching notification counter.
     */
    public synchronized static void increaseNotificationsCount() {
        matchingNotificationsCount++;
    }

    /**
     * Gets matching notification counter.
     *
     * @return Number of notifications processed by interface.
     */
    public static String getNotificationsCount() {
        return "Total notifications found: " + matchingNotificationsCount;
    }

    /**
     * Converts safely string to number.
     *
     * @param val String with number
     * @return Integer value of string
     */
    public static int getNumber(String val) {
        if (val == null || val.isEmpty()) {
            return 0;
        }

        int len = val.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(val.charAt(i))) {
                return 0;
            }
        }

        return Integer.parseInt(val);
    }

    /**
     *
     *
     * @param val String with e-mail
     * @return boolean Returns true if e-mail is valid
     */
    public static Boolean isValidEmail(String val) {
        Pattern pattern = Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?");
        Matcher matcher = pattern.matcher(val.toLowerCase());

        if (matcher.find()) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Checks if directory path is absolute.
     *
     * @param path Directory path
     * @return TRUE if path is absolute
     */
    private static Boolean isDirectory(String path) {
        if (path != null) {
            File test = new File(path);

            if (test.isAbsolute()) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else {
            return Boolean.FALSE;
        }
    }

    private static void getSenderEmail() {
        String tempValue;
        String[] tempArray;

        tempValue = properities.getProperty("new_sender");
        if (tempValue != null && tempValue.isEmpty() == false) {
            tempArray = tempValue.split(",");

            try {
                tempArray[0] = tempArray[0].trim().replace(NBSP, "");

                if (validate_email) {
                    if (isValidEmail(tempArray[0])) {
                        new_sender = new InternetAddress(tempArray[0], tempArray[0], encoding);
                    } else {
                        new_sender = null;

                        System.out.println("\r\nNotificationEmail - Warning! - Override value for sender is invalid!");
                    }
                } else {
                    new_sender = new InternetAddress(tempArray[0], tempArray[0], encoding);
                }
            } catch (Exception ex) {
                new_sender = null;

                System.out.println("\r\nNotificationEmail - Warning! - Overriding value for sender failed:\r\n" + ex.toString());
            }
        } else {
            new_sender = null;
        }
    }

    private static Address[] getOtherEmails(String property) {
        String tempValue;
        String[] tempArray;
        Address[] finalEmails;
        ArrayList<String> validEmailsList = new ArrayList<String>();

        tempValue = properities.getProperty(property);
        if (tempValue != null && tempValue.isEmpty() == false) {
            tempArray = tempValue.split(",");

            for (int i = 0; i < tempArray.length; i++) {
                tempArray[i] = tempArray[i].trim().replace(NBSP, "");

                if (validate_email) {
                    if (isValidEmail(tempArray[i])) {
                        if (!validEmailsList.contains(tempArray[i])) {
                            validEmailsList.add(tempArray[i]);
                        }
                    } else {
                        System.out.println("\r\nNotificationEmail - Warning! - Override value for " + property + ": " + tempArray[i] + " is invalid!");
                    }
                } else {
                    if (!validEmailsList.contains(tempArray[i])) {
                        validEmailsList.add(tempArray[i]);
                    }
                }
            }

            if (validEmailsList.size() > 0) {
                finalEmails = new Address[validEmailsList.size()];

                for (int i = 0; i < validEmailsList.size(); i++) {
                    try {
                        finalEmails[i] = new InternetAddress(validEmailsList.get(i), validEmailsList.get(i), encoding);
                    } catch (Exception ex) {
                        System.out.println("\r\nNotificationEmail - Warning! - Overriding values for " + property + " failed:\r\n" + ex.toString());
                    }
                }

                if (finalEmails.length > 0) {
                    return finalEmails;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
