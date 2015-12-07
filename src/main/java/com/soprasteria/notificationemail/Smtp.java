package com.soprasteria.notificationemail;

import com.sun.mail.smtp.SMTPAddressFailedException;
import com.sun.mail.smtp.SMTPSenderFailedException;
import com.sun.mail.util.MailConnectException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import org.apache.log4j.Logger;

/**
 * Class that represent SMTP object.
 *
 * @author sgacka
 */
public class Smtp {

    private final String login;
    private final String password;
    private final String host;
    private final int port;
    private final boolean requiresAuth;
    private final boolean useSSL;
    private final boolean sendHtml;
    private Session session;
    private Message message;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final PrintStream ps = new PrintStream(baos);
    private String smtpLog;
    private static final Logger logger = Logger.getLogger(Smtp.class);
    private static final Logger smtpLogger = Logger.getLogger("SmtpLog");

    /**
     * SMTP constructor
     *
     * @param smtpLogin Login used by SMTP server
     * @param smtpPassword User password
     * @param smtpHost Host name/address
     * @param smtpPort Port number
     * @param smtpRequiresAuth Information if authorization is required
     * @param smtpUseSSL Information if connection uses SSL
     * @param sendHtml Information if body is in HTML
     */
    public Smtp(String smtpLogin, String smtpPassword, String smtpHost, int smtpPort, boolean smtpRequiresAuth, boolean smtpUseSSL, boolean sendHtml) {
        this.login = smtpLogin;
        this.password = smtpPassword;
        this.host = smtpHost;
        this.port = smtpPort;
        this.requiresAuth = smtpRequiresAuth;
        this.useSSL = smtpUseSSL;
        this.sendHtml = sendHtml;

        setProperities();
    }

    /**
     * Sets SMTP properties - authorization, SSL, etc.
     */
    private void setProperities() {
        Properties smtpProps = new Properties();

        smtpProps.put("mail.from", login);

        if (useSSL == true) {
            smtpProps.put("mail.transport.protocol", "smtps");
            smtpProps.put("mail.smtp.ssl.enable", true);
//            smtpProps.put("mail.smtp.socketFactory.port", port);
//            smtpProps.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
            smtpProps.put("mail.smtp.auth", true);
        } else {
            smtpProps.put("mail.transport.protocol", "smtp");
            smtpProps.put("mail.smtp.auth", requiresAuth);
        }

//        smtpProps.put("mail.smtp.starttls.enable", true);
        smtpProps.put("mail.smtp.host", host);
        smtpProps.put("mail.smtp.port", port);

        if (requiresAuth == true) {
            Authenticator auth = new SMTPAuthenticator();

            session = Session.getDefaultInstance(smtpProps, auth);
        } else {
            session = Session.getDefaultInstance(smtpProps, null);
        }

        // Set debug option
        if (logger.isDebugEnabled()) {
            session.setDebug(true);
            session.setDebugOut(ps);
        }
    }

    /**
     * Sends email to destination addresses.
     *
     * @param email Email object
     * @return Sending status
     */
    public String sendEmail(Email email) {
        String smtpStatus;

        message = new MimeMessage(session);

        try {
            message.setFrom(email.getSender(Configuration.encoding));
            message.addRecipients(Message.RecipientType.TO, email.getRecipients(Configuration.encoding));
            message.addRecipients(Message.RecipientType.CC, email.getCC(Configuration.encoding));
            message.addRecipients(Message.RecipientType.BCC, email.getBCC(Configuration.encoding));
            message.setSubject(email.getSubject(Configuration.encoding));
            message.setSentDate(email.getSentDate().getTime());

            if (sendHtml) {
                MimeMultipart content = new MimeMultipart("alternative");
                MimeBodyPart text = new MimeBodyPart();
                MimeBodyPart html = new MimeBodyPart();

                text.setText(email.getContentText(), Configuration.encoding);
                html.setContent(email.getContentHtml(), "text/html; charset=" + Configuration.encoding);

                content.addBodyPart(text);
                content.addBodyPart(html);

                message.setContent(content);
            } else {
                message.setContent(email.getContentText(), "text/plain; charset=" + Configuration.encoding);
            }

            if (logger.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                Enumeration headers = message.getAllHeaders();

                while (headers.hasMoreElements()) {
                    Header header = (Header) headers.nextElement();
                    sb.append(header.getName());
                    sb.append(" ");
                    sb.append(header.getValue());
                    sb.append("\r\n");
                }

                logger.debug(getRecordNumber(email) + "Email headers:\r\n" + sb.toString().trim());
            }

            // Send the message
            Transport.send(message);

            smtpStatus = "SUCCESS";

            createLogMessage(smtpStatus, email, "");
        } catch (Throwable e) {
            Exception ex = (Exception) e;
            String exceptionMessage;

            if (ex instanceof MailConnectException) {
                smtpStatus = "CONNECTION_REFUSED";
                exceptionMessage = ex.getMessage();

                logger.error(getRecordNumber(email) + "Unable to connect", ex);
            } else if (ex instanceof AuthenticationFailedException) {
                smtpStatus = "SMTP_AUTHENTICATION_FAILED";
                exceptionMessage = ex.getMessage();

                logger.error(getRecordNumber(email) + "SMTP authentication failed", ex);
            } else {
                do {
                    if (ex instanceof SendFailedException) {
                        SendFailedException sfex = (SendFailedException) ex;

                        boolean senderStatus = false;

                        if (sfex instanceof SMTPAddressFailedException) {
                            String returnCode = sfex.getMessage();
                            if (returnCode != null && returnCode.length() > 0) {
                                returnCode = returnCode.trim();
                            } else {
                                returnCode = "";
                            }

                            if (returnCode.equals("530 SMTP authentication is required.")) {
                                smtpStatus = "SMTP_REQUIRES_AUTHENTICATION";
                                exceptionMessage = sfex.getMessage();

                                logger.error(getRecordNumber(email) + "SMTP requires authentication", sfex);
                            } else if (returnCode.equals("550 Unknown user")) {
                                smtpStatus = "FAILURE";
                                exceptionMessage = sfex.getMessage();

                                logger.error(getRecordNumber(email) + "E-mail address not found on the server or rejected due to policy reasons", sfex);
                            } else {
                                smtpStatus = "FAILURE";
                                exceptionMessage = ex.getMessage();

                                logger.error(getRecordNumber(email) + "Cannot send e-mail", ex);
                            }

                            break;
                        } else if (sfex instanceof SMTPSenderFailedException) {
                            exceptionMessage = sfex.getMessage();

                            logger.error(getRecordNumber(email) + "Sender e-mail is invalid!", sfex);
                        } else {
                            senderStatus = true;
                            exceptionMessage = "";
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("Invalid addresses: ");
                        Address[] invalid = sfex.getInvalidAddresses();
                        if (invalid != null && invalid.length > 0) {
                            for (int i = 0; i < invalid.length; i++) {
                                sb.append(invalid[i]);
                                if (i != invalid.length - 1) {
                                    sb.append("; ");
                                }
                            }
                            logger.error(getRecordNumber(email) + sb.toString());
                        }

                        sb = new StringBuilder();
                        sb.append("Valid but not sent addresses: ");
                        Address[] validUnsent = sfex.getValidUnsentAddresses();
                        if (validUnsent != null && validUnsent.length > 0) {
                            for (int i = 0; i < validUnsent.length; i++) {
                                sb.append(validUnsent[i]);
                                if (i != validUnsent.length - 1) {
                                    sb.append("; ");
                                }
                            }
                            logger.warn(getRecordNumber(email) + sb.toString());
                        }

                        sb = new StringBuilder();
                        sb.append("Valid and sent addresses: ");
                        Address[] validSent = sfex.getValidSentAddresses();
                        if (validSent != null && validSent.length > 0) {
                            for (int i = 0; i < validSent.length; i++) {
                                sb.append(validSent[i]);
                                if (i != validSent.length - 1) {
                                    sb.append("; ");
                                }
                            }
                            logger.debug(getRecordNumber(email) + sb.toString());
                        }

                        if (senderStatus) {
                            if ((invalid != null || validUnsent != null) && validSent == null) {
                                smtpStatus = "FAILURE";
                            } else {
                                smtpStatus = "PARTIAL_SUCCESS";
                            }
                        } else {
                            smtpStatus = "FAILURE";
                        }
                    } else if (ex instanceof UnknownHostException) {
                        smtpStatus = "UNKNOWN_HOST";
                        exceptionMessage = ex.getMessage();

                        logger.error(getRecordNumber(email) + "Unable to find host", ex);
                    } else {
                        smtpStatus = "FAILURE";
                        exceptionMessage = ex.getMessage();

                        logger.error(getRecordNumber(email) + "Cannot send e-mail", ex);
                    }

                    if (ex instanceof MessagingException) {
                        ex = ((MessagingException) ex).getNextException();
                    } else {
                        ex = null;
                    }
                } while (ex != null);
            }

            createLogMessage(smtpStatus, email, exceptionMessage);
        } finally {
            //addLogEntryToFile();
            smtpLogger.info(smtpLog);

            if (logger.isTraceEnabled() && baos.size() > 0) {
                logger.trace(getRecordNumber(email) + "SMTP debug:\r\n" + baos.toString());
            }

            // wait 1s to close files
            try {
                Thread.sleep(1000);
            } catch (Throwable e) {
                // nothing
            }
        }

        return smtpStatus;
    }

    /**
     * Creates log message with basic email informations.
     *
     * @throws Exception
     */
    private void createLogMessage(String success, Email email, String error) {
        try {
            StringBuilder sb = new StringBuilder();
            String sender = "";
            String recipients = "";
            String cc = "";
            String bcc = "";
            String subject = "";
            Enumeration headers = message.getAllHeaders();

            while (headers.hasMoreElements()) {
                Header header = (Header) headers.nextElement();

                if (header.getName().equals("From")) {
                    sender = MimeUtility.decodeText(header.getValue());
                } else if (header.getName().equals("To")) {
                    recipients = MimeUtility.decodeText(header.getValue());
                } else if (header.getName().equals("Cc")) {
                    cc = MimeUtility.decodeText(header.getValue());
                } else if (header.getName().equals("Bcc")) {
                    bcc = MimeUtility.decodeText(header.getValue());
                } else if (header.getName().equals("Subject")) {
                    subject = MimeUtility.decodeText(header.getValue());
                }
            }

            sb.append(email.getEventID());
            sb.append(";");
            sb.append(email.getEventOperator());
            sb.append(";");
            sb.append(email.getCompany());
            sb.append(";");
            sb.append(email.getTicketNumbers());
            sb.append(";");
            sb.append(sender);
            sb.append(";");
            sb.append(recipients);
            sb.append(";");
            sb.append(cc);
            sb.append(";");
            sb.append(bcc);
            sb.append(";");
            sb.append(subject.replace("\r\n", " "));
            sb.append(";");
            sb.append(success);
            sb.append(";");
            sb.append(error);
            // message is empty

            smtpLog = sb.toString();
        } catch (Throwable e) {
            logger.error(getRecordNumber(email) + "Unable to create SMTP log entry", e);
        }
    }

    /**
     * Gets formatted eventout record number for logger.
     *
     * @return Formatted eventout record number
     */
    private String getRecordNumber(Email email) {
        return "<" + email.getEventID() + "> -> ";
    }

    /**
     * SMTPAuthenticator object used for SMTP authorization.
     */
    private class SMTPAuthenticator extends javax.mail.Authenticator {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(login, password);
        }
    }
}
