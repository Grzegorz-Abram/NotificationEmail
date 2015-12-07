package com.soprasteria.notificationemail;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

/**
 * Class that represents eventout record.
 *
 * @author sgacka
 */
public class EventOutRecord implements Runnable {

    private final Clob evFields;
    private final Calendar evTime;
    private final String evSysSeq;
    private final Company companyRecords;
    private final Database database;
    private final Email email;
    private String subject;
    private Address sender;
    private Address[] recipients;
    private Address[] cc;
    private Address[] bcc;
    private String finalText;
    private String finalHtml;
    private String company;
    private String ticketNumbers;
    private String eventOperator;
    private static final Logger logger = Logger.getLogger(EventOutRecord.class);

    /**
     * EventOutRecord constructor.
     *
     * @param evFields Field with message from notification
     * @param evTime Calendar of the event
     * @param evSysSeq Unique event key
     * @param companyReocrds Company object
     * @param database Database object
     */
    public EventOutRecord(Clob evFields, long evTime, String evSysSeq, Company companyReocrds, Database database) {
        this.evFields = evFields;
        this.evTime = Calendar.getInstance();
        this.evTime.setTimeInMillis(evTime);
        this.evSysSeq = evSysSeq;
        this.companyRecords = companyReocrds;
        this.database = database;

        this.email = new Email();
    }

    /**
     * Sends email with provided content.
     *
     * @throws Exception
     */
    private void sendEmail() throws Exception {
        boolean sendHtml = false;

        if (finalHtml != null) {
            sendHtml = true;
        }
        email.setSubject(subject);
        email.setContentText(finalText);
        email.setContentHtml(finalHtml);
        email.setSender(sender);
        email.setRecipients(recipients);
        email.setCC(cc);
        email.setBCC(bcc);
        email.setCompany(company);
        email.setTicketNumbers(ticketNumbers);
        email.setEventID(evSysSeq);
        email.setEventOperator(eventOperator);
        email.setSentDate(evTime);

        Smtp smtp = new Smtp(Configuration.smtp_user, Configuration.smtp_password, Configuration.smtp_host, Configuration.smtp_port, Configuration.smtp_req_auth, Configuration.smtp_ssl, sendHtml);
        String smtpStatus = smtp.sendEmail(email);

        if (smtpStatus.equals("SUCCESS")) {
            // remove record from table
            if (!Configuration.isReadOnly) {
                database.removeRecordFromEventOut(evSysSeq);
            }
        } else if (smtpStatus.equals("FAILURE") || smtpStatus.equals("PARTIAL_SUCCESS")) {
            // update record with status
            if (!Configuration.isReadOnly) {
                if (smtpStatus.equals("FAILURE")) {
                    database.markRecord(evSysSeq, "error");
                } else {
                    database.markRecord(evSysSeq, "warning");
                }
            }
        }
    }

    /**
     * Run method for ThreadExecutor. Executes event record parsing.
     */
    public void run() {
        try {
            parse();
        } catch (Throwable e) {
            logger.error(getRecordNumber() + "Unable to parse eventout record:\r\n", e);
        }
    }

    /**
     * Parses eventout record and send e-mail.
     *
     * @throws Exception
     */
    public void parse() throws Exception {
        String targetRecipients = "";
        String targetSubject = "";
        String targetBody;
        StringBuilder overriddenEmails = new StringBuilder();

        if (evFields != null && evFields.length() > 0) {
            String temp = evFields.getSubString(1, (int) evFields.length());
            int currPos = 0;
            int oldPos = 0;

            for (int i = 0; i < 4; i++) {
                currPos = temp.indexOf(Configuration.separator, currPos);

                switch (i) {
                    case 0:
                        targetRecipients = temp.substring(0, currPos);
                        logger.trace(getRecordNumber() + "Recipient(s) found: " + targetRecipients);

                        break;
                    case 1:
                        eventOperator = temp.substring(oldPos, currPos);
                        logger.trace(getRecordNumber() + "Event operator found: " + eventOperator);

                        break;
                    case 3:
                        targetSubject = temp.substring(oldPos, currPos);
                        logger.trace(getRecordNumber() + "Subject found: " + targetSubject);

                        break;
                    default:
                        break;
                }

                currPos += 1;
                oldPos = currPos;
            }

            targetBody = temp.substring(oldPos, temp.length());
            logger.trace(getRecordNumber() + "Body found: " + targetBody);

            if (targetSubject.equals(targetBody)) {
                targetSubject = "";
                logger.warn(getRecordNumber() + "Subject is missing in notification record!");
            } else if (targetSubject.startsWith("Message - Could not be found:")) {
                targetSubject = "";
                logger.warn(getRecordNumber() + "Subject message is missing!");
            } else if (targetSubject.startsWith("This message did not provide enough arguments:")) {
                targetSubject = "";
                logger.warn(getRecordNumber() + "Subject message did not provide enough arguments!");
            }

            if (targetBody.startsWith("Message - Could not be found:")) {
                database.markRecord(evSysSeq, "error");

                logger.error(getRecordNumber() + "Body message is missing and record will be ignored!");
            } else if (targetBody.startsWith("This message did not provide enough arguments:")) {
                database.markRecord(evSysSeq, "error");

                logger.error(getRecordNumber() + "Body message did not provide enough arguments and record will be ignored!");
            } else {
                int firstLine = targetBody.indexOf("\n");
                if (firstLine < 0) {
                    firstLine = targetBody.length();
                }
                logger.debug(getRecordNumber() + "First line: " + targetBody.substring(0, firstLine));

                String[] company_to_array = targetBody.substring(0, firstLine).split("\\|");
                company = company_to_array[0].trim().replace(Configuration.NBSP, "");

                CompanyRecord companyRecord = companyRecords.getCompanyRecord(company);
                if (companyRecord != null) {
                    if ((Configuration.include_companies == null || Configuration.include_companies.contains(companyRecord.getCompany()))
                            && (Configuration.exclude_companies == null || !Configuration.exclude_companies.contains(companyRecord.getCompany()))) {
                        logger.info(getRecordNumber() + "Processing record for company " + companyRecord.getCompany());

                        // increase notifications count
                        Configuration.increaseNotificationsCount();

                        // setting email addresses
                        // sender
                        if (Configuration.new_sender == null) {
                            if (companyRecord.getSenderEmail() != null) {
                                if (companyRecord.getSenderName() != null) {
                                    sender = new InternetAddress(companyRecord.getSenderEmail(), companyRecord.getSenderName(), Configuration.encoding);
                                } else {
                                    sender = new InternetAddress(companyRecord.getSenderEmail(), companyRecord.getSenderEmail(), Configuration.encoding);
                                }
                            }
                        } else {
                            overriddenEmails.append("Original sender:\t");
                            overriddenEmails.append(companyRecord.getSenderEmail());
                            overriddenEmails.append("\r\n");

                            sender = Configuration.new_sender;

                        }

                        if (sender != null) {
                            // recipients
                            if (Configuration.new_recipients == null) {
                                String[] recipientsArray = targetRecipients.split(";");
                                recipients = new Address[recipientsArray.length];

                                for (int i = 0; i < recipientsArray.length; i++) {
                                    recipients[i] = new InternetAddress(recipientsArray[i].trim().replace(Configuration.NBSP, ""), recipientsArray[i].trim().replace(Configuration.NBSP, ""), Configuration.encoding);
                                }
                            } else {
                                overriddenEmails.append("Original recipient(s):\t");
                                overriddenEmails.append(targetRecipients);
                                overriddenEmails.append("\r\n");

                                recipients = Configuration.new_recipients;
                            }

                            // cc
                            if (Configuration.new_cc == null) {
                                if (company_to_array.length > 1) {
                                    String[] ccArray = splitEmailArray(company_to_array[1], ",");
                                    cc = new Address[ccArray.length];

                                    for (int i = 0; i < ccArray.length; i++) {
                                        cc[i] = new InternetAddress(ccArray[i], ccArray[i], Configuration.encoding);
                                    }
                                }
                            } else {
                                if (company_to_array.length > 1) {
                                    overriddenEmails.append("Original cc(s):\t\t");
                                    overriddenEmails.append(company_to_array[1].replace(",", "; "));
                                    overriddenEmails.append("\r\n");
                                }

                                cc = Configuration.new_cc;
                            }

                            // bcc
                            if (Configuration.new_bcc == null) {
                                if (company_to_array.length > 2) {
                                    String[] bccArray = splitEmailArray(company_to_array[2], ",");
                                    bcc = new Address[bccArray.length];

                                    for (int i = 0; i < bccArray.length; i++) {
                                        bcc[i] = new InternetAddress(bccArray[i], bccArray[i], Configuration.encoding);
                                    }
                                }
                            } else {
                                if (company_to_array.length > 2) {
                                    overriddenEmails.append("Original bcc(s):\t");
                                    overriddenEmails.append(company_to_array[2].replace(",", "; "));
                                    overriddenEmails.append("\r\n");
                                }

                                bcc = Configuration.new_bcc;
                            }

                            getTicketNumbers(targetSubject);

                            subject = targetSubject.trim().replace(Configuration.NBSP, " ");

                            if (targetBody.length() > 0 && firstLine != targetBody.length()) {
                                targetBody = targetBody.substring(firstLine + 1, targetBody.length());
                            } else {
                                targetBody = "";
                            }

                            // prepare string for overriden e-mails
                            if (overriddenEmails.length() > 0) {
                                overriddenEmails.insert(0, "\r\n\r\n---------------------------------------------------------------------\r\n");
                            }

                            setBody(targetBody.trim().replace(Configuration.NBSP, " "), overriddenEmails.toString());

                            sendEmail();
                        } else {
                            database.markRecord(evSysSeq, "error");

                            logger.error(getRecordNumber() + "Sender e-mail was not provided!");
                        }
                    } else {
                        logger.info(getRecordNumber() + "Record is for company " + companyRecord.getCompany() + " and will be ignored.");
                    }
                } else {
                    database.markRecord(evSysSeq, "error");

                    logger.error(getRecordNumber() + "Record didn't provide valid company name!");
                }
            }
        } else {
            database.markRecord(evSysSeq, "error");

            logger.error(getRecordNumber() + "Record is empty!");
        }
    }

    /**
     * Sets final e-mail body (text and HTML if valid)
     *
     * @param body
     * @param overriddenEmails
     */
    public void setBody(String body, String overriddenEmails) {
        if (body.toLowerCase().startsWith("<html") && body.toLowerCase().endsWith("</html>")) {
            if (overriddenEmails.length() > 0) {
                int lastHtmlClose = body.toLowerCase().lastIndexOf("</html>");
                body = body.substring(0, lastHtmlClose);
                body = body + "<pre>" + overriddenEmails + "</pre></html>";
            }
            finalText = Jsoup.parse(body).text();
            finalHtml = body;
        } else {
            finalText = body + overriddenEmails;
        }
    }

    /**
     * Gets ticket number from subject if present. First number that has been
     * found will be used in log entry.
     *
     * @param targetSubject
     */
    private void getTicketNumbers(String targetSubject) {
        Pattern pattern = Pattern.compile("(SD|IM|RQ|OM|CM|CT|KM|KE|PM|DT)\\d{9}(-\\d{3})?");
        Matcher matcher = pattern.matcher(targetSubject);

        StringBuilder numbers = new StringBuilder();

        while (matcher.find()) {
            numbers.append(matcher.group()).append(",");
        }

        if (numbers.length() > 0) {
            ticketNumbers = numbers.substring(0, numbers.length() - 1);
        } else {
            ticketNumbers = "Ticket number not found.";
        }
    }

    /**
     * Splits one string with separated e-mails to string array
     *
     * @param emails List of e-mails separated by provided separator
     * @param separator Separator used to separate e-mail addresses
     * @return Array of e-mails
     */
    private String[] splitEmailArray(String emails, String separator) {
        String[] emailArray = emails.trim().split(separator);

        for (int i = 0; i < emailArray.length; i++) {
            emailArray[i] = emailArray[i].trim().replace(Configuration.NBSP, "");
        }

        ArrayList<String> emailList = new ArrayList<String>(Arrays.asList(emailArray));

        emailList.removeAll(Arrays.asList("", null));

        return emailList.toArray(new String[emailList.size()]);
    }

    /**
     * Gets formatted eventout record number for logger.
     *
     * @return Formatted eventout record number
     */
    private String getRecordNumber() {
        return "<" + evSysSeq + "> -> ";
    }
}
