package com.soprasteria.notificationemail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;

/**
 * Class that represents e-mail message.
 *
 * @author sgacka
 */
public class Email {

    private String subject;
    private String bodyText;
    private String bodyHtml;
    private String[] from;
    private final ArrayList<String[]> cc;
    private final ArrayList<String[]> bcc;
    private final ArrayList<String[]> to;
    private Calendar date;
    private String ID;
    private String fileNamePrefix;
    private String company;
    private String ticketNumbers;
    private String eventID;
    private String eventOperator;
    private String destinationPath;
    private String backupPath;
    private static final Logger logger = Logger.getLogger(Email.class);

    /**
     * Email constructor.
     */
    public Email() {
        from = new String[2];
        to = new ArrayList<String[]>();
        cc = new ArrayList<String[]>();
        bcc = new ArrayList<String[]>();
    }

    /**
     * Sets sender e-mail address.
     *
     * @param from Sender e*mail
     */
    public void setSender(Address from) {
        this.from = parseEmail(from.toString());
    }

    /**
     * Sets cc e-mail addresses.
     *
     * @param cc Cc e-mails
     */
    public void setCC(Address[] cc) {
        if (cc != null) {
            for (int i = 0; i < cc.length; i++) {
                this.cc.add(parseEmail(cc[i].toString()));
            }
        }
    }

    /**
     * Sets bcc e-mail addresses.
     *
     * @param bcc Bcc e-mails
     */
    public void setBCC(Address[] bcc) {
        if (bcc != null) {
            for (int i = 0; i < bcc.length; i++) {
                this.bcc.add(parseEmail(bcc[i].toString()));
            }
        }
    }

    /**
     * Sets recipients e-mail addresses.
     *
     * @param to Recipients e-mails
     */
    public void setRecipients(Address[] to) {
        if (to != null) {
            for (int i = 0; i < to.length; i++) {
                this.to.add(parseEmail(to[i].toString()));
            }
        }
    }

    /**
     * Returns Address object with sender e-mail address encoded with provided
     * charset.
     *
     * @param charset The name of charset
     * @return Sender e-mail address
     */
    public Address getSender(String charset) {
        Address address = null;

        try {
            address = new InternetAddress(from[0], from[1], charset);
        } catch (Throwable e) {
            logger.error(getRecordNumber() + "Unable to get sender", e);
        }

        return address;
    }

    /**
     * Returns Address array object with cc e-mail addresses encoded with
     * provided charset.
     *
     * @param charset The name of charset
     * @return Cc e-mail addresses
     */
    public Address[] getCC(String charset) {
        Address[] addresses = new Address[cc.size()];

        if (logger.isTraceEnabled()) {
            logger.trace(getRecordNumber() + "Total CCs: " + cc.size());
        }

        for (int i = 0; i < cc.size(); i++) {
            try {
                addresses[i] = new InternetAddress(cc.get(i)[0], cc.get(i)[1], charset);
            } catch (Throwable e) {
                logger.error(getRecordNumber() + "Unable to get CC", e);
            }
        }

        return addresses;
    }

    /**
     * Returns Address array object with bcc e-mail addresses encoded with
     * provided charset.
     *
     * @param charset The name of charset
     * @return Bcc e-mail addresses
     */
    public Address[] getBCC(String charset) {
        Address[] addresses = new Address[bcc.size()];

        if (logger.isTraceEnabled()) {
            logger.trace(getRecordNumber() + "Total BCCs: " + bcc.size());
        }

        for (int i = 0; i < bcc.size(); i++) {
            try {
                addresses[i] = new InternetAddress(bcc.get(i)[0], bcc.get(i)[1], charset);
            } catch (Throwable e) {
                logger.error(getRecordNumber() + "Unable to get BCC", e);
            }
        }

        return addresses;
    }

    /**
     * Returns Address array object with recipients e-mail addresses encoded
     * with provided charset.
     *
     * @param charset The name of charset
     * @return Recipients e-mail addresses
     */
    public Address[] getRecipients(String charset) {
        Address[] addresses = new Address[to.size()];

        if (logger.isTraceEnabled()) {
            logger.trace(getRecordNumber() + "Total recipients: " + to.size());
        }

        for (int i = 0; i < to.size(); i++) {
            try {
                addresses[i] = new InternetAddress(to.get(i)[0], to.get(i)[1], charset);
            } catch (Throwable e) {
                logger.error(getRecordNumber() + "Unable to get recipients", e);
            }
        }

        return addresses;
    }

    /**
     * Return e-mail subject encoded with provided charset.
     *
     * @param charset The name of charset
     * @return E-mail subject
     */
    public String getSubject(String charset) {
        return encodeText(subject, charset);
    }

    /**
     * Return e-mail body in text format.
     *
     * @return E-mail body (text)
     */
    public String getContentText() {
        return bodyText;
    }

    /**
     * Returns e-mail body in HTML format.
     *
     * @return E-mail body (HTML)
     */
    public String getContentHtml() {
        return bodyHtml;
    }

    /**
     * Returns e-mail ID
     *
     * @return E-mail ID
     */
    public String getID() {
        return ID;
    }

    /**
     * Sets text e-mail body.
     *
     * @param bodyText Body message (text)
     */
    public void setContentText(String bodyText) {
        if (bodyText != null) {
            this.bodyText = bodyText;
        } else {
            this.bodyText = "";
        }
    }

    /**
     * Sets HTML e-mail body.
     *
     * @param bodyHtml Body message (HTML)
     */
    public void setContentHtml(String bodyHtml) {
        if (bodyHtml != null) {
            this.bodyHtml = bodyHtml;
        } else {
            this.bodyHtml = "";
        }
    }

    /**
     * Sets e-mail subject.
     *
     * @param subject E-mail subject
     */
    public void setSubject(String subject) {
        if (subject != null) {
            this.subject = subject;
        } else {
            this.subject = "";
        }
    }

    /**
     * Gets e-mail sent date.
     *
     * @return
     */
    public Calendar getSentDate() {
        return date;
    }

    /**
     * Sets e-mail sent date.
     *
     * @param date E-mail sent date
     */
    public void setSentDate(Calendar date) {
        this.date = date;
    }

    /**
     * Sets company name.
     *
     * @param company
     */
    public void setCompany(String company) {
        this.company = company;
    }

    /**
     * Gets company name.
     *
     * @return Company name
     */
    public String getCompany() {
        return company;
    }

    /**
     * Sets notification ticket number.
     *
     * @param ticketNumbers
     */
    public void setTicketNumbers(String ticketNumbers) {
        this.ticketNumbers = ticketNumbers;
    }

    /**
     * Gets notification ticket number.
     *
     * @return Ticket number
     */
    public String getTicketNumbers() {
        return ticketNumbers;
    }

    /**
     * Sets notification event ID.
     *
     * @param eventID
     */
    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    /**
     * Gets notification event ID.
     *
     * @return Notification event ID
     */
    public String getEventID() {
        return eventID;
    }

    /**
     * Sets event operator name
     *
     * @param eventOperator
     */
    public void setEventOperator(String eventOperator) {
        this.eventOperator = eventOperator;
    }

    /**
     * Gets event operator name
     *
     * @return Name of operator that triggered notification
     */
    public String getEventOperator() {
        return eventOperator;
    }

    /**
     * Sets path where e-mail will be stored.
     *
     * @param destinationPath Destination path for e-mail message
     */
    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    /**
     * Sets path where e-mail backup will be stored.
     *
     * @param backupPath Backup path for e-mail
     */
    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    /**
     * Sets message ID based on JavaMail ID
     *
     * @param messageID JavaMail ID
     */
    public void setMessageID(String messageID) {
        this.ID = messageID;
    }

    /**
     * Return file destination path.
     *
     * @return Path to store file
     */
    public String getDestinationPath() {
        return destinationPath;
    }

    /**
     * Makes simple e-mail backup.
     *
     * @param backupPath Destination path for backup
     */
    public void simpleBackupEmail(String backupPath) {
        String currentDate = convertDate(3);

        try {
            File path = new File(backupPath + File.separator + currentDate);
            path.mkdirs();

            // BOM for utf-8
            byte[] bom = new byte[3];
            bom[0] = (byte) 0xEF;
            bom[1] = (byte) 0xBB;
            bom[2] = (byte) 0xBF;

            File file = new File(path.getPath() + File.separator + fileNamePrefix + "email.ticket");

            PrintWriter pw = new PrintWriter(file, "utf-8");
            // add BOM at the begginning
            pw.write(new String(bom, "utf-8"));
            pw.write(bodyText);

            pw.flush();
            pw.close();
        } catch (Throwable e) {
            logger.error(getRecordNumber() + "Unable to make sipmle e-mail backup", e);
        }
    }

    /**
     * Makes backup of entire e-mail with attachments
     */
    public void backupEmail() {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder domBuilder = domFactory.newDocumentBuilder();

            Element element;
            Node node;

            // root - Message
            Document newXML = domBuilder.newDocument();
            Element rootElement = newXML.createElement("Message");
            newXML.appendChild(rootElement);

            // Message -> Subject
            element = newXML.createElement("Subject");
            node = newXML.createTextNode(subject);
            element.appendChild(node);
            rootElement.appendChild(element);

            // Message -> Body
            element = newXML.createElement("Body");
            node = newXML.createTextNode(bodyText);
            element.appendChild(node);
            rootElement.appendChild(element);

            // Message -> Date
            element = newXML.createElement("Date");
            node = newXML.createTextNode(convertDate(3));
            element.appendChild(node);
            rootElement.appendChild(element);

            // Message -> From
            rootElement.appendChild(xmlEmail(newXML, "From", from));

            // Message -> To
            rootElement.appendChild(xmlEmail(newXML, "To", to));

            // Message -> Cc
            rootElement.appendChild(xmlEmail(newXML, "Cc", cc));

            // Message -> Bcc
            rootElement.appendChild(xmlEmail(newXML, "Bcc", bcc));

            // save XML to file
            File path = new File(backupPath + File.separator + convertDate(3));
            path.mkdirs();

            File file = new File(path.getPath() + File.separator + fileNamePrefix + "email.xml");
            FileOutputStream fos = new FileOutputStream(file);

            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");

            LSSerializer serializer = impl.createLSSerializer();
            serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);

            LSOutput output = impl.createLSOutput();
            output.setByteStream(fos);
            output.setEncoding("utf-8");

            // BOM for utf-8
            byte[] bom = new byte[3];
            bom[0] = (byte) 0xEF;
            bom[1] = (byte) 0xBB;
            bom[2] = (byte) 0xBF;

            fos.write(bom);
            serializer.write(newXML, output);
        } catch (Throwable e) {
            logger.error(getRecordNumber() + "Unable to make e-mail backup", e);
        }
    }

    /**
     * Returns XML element with e-mail addresses
     *
     * @param xml XML document
     * @param name Name of the element that will be created
     * @param list List of e-mail addresses
     * @return XML element
     */
    private Element xmlEmail(Document xml, String name, ArrayList<String[]> list) {
        Element subElement1;
        Element subElement2;
        Node node;
        String[] temp;

        Element element = xml.createElement(name);

        for (int i = 0; i < list.size(); i++) {
            temp = list.get(i);

            // From -> Email
            subElement1 = xml.createElement("Email");
            element.appendChild(subElement1);

            // Email -> Name
            subElement2 = xml.createElement("Name");
            node = xml.createTextNode(temp[0]);
            subElement2.appendChild(node);
            subElement1.appendChild(subElement2);

            // Email -> Address
            subElement2 = xml.createElement("Address");
            node = xml.createTextNode(temp[1]);
            subElement2.appendChild(node);
            subElement1.appendChild(subElement2);
        }

        list.clear();

        return element;
    }

    /**
     * Returns XML element with e-mail address
     *
     * @param xml XML document
     * @param name Name of the element that will be created
     * @param array Array with e-mail address
     * @return XML element
     */
    private Element xmlEmail(Document xml, String name, String[] array) {
        Element subElement1;
        Element subElement2;
        Node node;

        Element element = xml.createElement(name);

        // From -> Email
        subElement1 = xml.createElement("Email");
        element.appendChild(subElement1);

        // Email -> Name
        subElement2 = xml.createElement("Name");
        node = xml.createTextNode(array[0]);
        subElement2.appendChild(node);
        subElement1.appendChild(subElement2);

        // Email -> Address
        subElement2 = xml.createElement("Address");
        node = xml.createTextNode(array[1]);
        subElement2.appendChild(node);
        subElement1.appendChild(subElement2);

        return element;
    }

    /**
     * Sets prefix used for storing files on disk. It's added at the beginning
     * of file name.
     */
    public void setFileNamePrefix() {
        fileNamePrefix = convertDate(1) + "_" + ID + "_";
    }

    /**
     * Parse e-mail address. Returns string array with e-mail address and its
     * description.
     *
     * @param email E-mail address
     * @return E-mail address and its description
     */
    private String[] parseEmail(String email) {
        email = decodeText(email);

        String[] temp = new String[2];

        int emailBegin = email.lastIndexOf("<");
        int emailEnd = email.lastIndexOf(">");

        if (emailBegin > 0 && emailEnd > 0) {
            temp[0] = email.substring(emailBegin + 1, emailEnd);
            temp[1] = email.substring(0, emailBegin).replace("\"", "").trim();
        } else {
            temp[0] = email;
            temp[1] = "";
        }

        return temp;
    }

    /**
     * Encode text using specified charset.
     *
     * @param text Text to encode
     * @param charset The name of charset
     * @return Encoded text
     */
    private String encodeText(String text, String charset) {
        String encodedText;

        try {
            encodedText = MimeUtility.encodeText(text, charset, null);
        } catch (Throwable e) {
            encodedText = text;

            logger.error(getRecordNumber() + "Enconding problem - using default charset", e);
        }

        return encodedText;
    }

    /**
     * Decode text to Java string.
     *
     * @param text Text to decode
     * @return Decoded text
     */
    private String decodeText(String text) {
        String decodedText;

        try {
            decodedText = MimeUtility.decodeText(text);
        } catch (Throwable e) {
            decodedText = text;

            logger.error(getRecordNumber() + "Decoding problem - using default charset", e);
        }

        return decodedText;
    }

    /**
     * Returns formatted reception date. Available formats: 1 - YYYYMMDDHHMISS 2
     * - DD/MM/YYYY HH:MI:SS 3 - YYYYMMDD
     *
     * @param format Number of format
     * @return Formatted date
     */
    private String convertDate(int format) {
        StringBuilder sb = new StringBuilder();

        String year = Integer.toString(date.get(Calendar.YEAR));
        String month;
        String day;
        String hour;
        String minute;
        String second;

        if ((date.get(Calendar.MONTH) + 1) < 10) {
            month = "0" + Integer.toString(date.get(Calendar.MONTH) + 1);
        } else {
            month = Integer.toString(date.get(Calendar.MONTH) + 1);
        }

        if ((date.get(Calendar.DAY_OF_MONTH)) < 10) {
            day = "0" + Integer.toString(date.get(Calendar.DAY_OF_MONTH));
        } else {
            day = Integer.toString(date.get(Calendar.DAY_OF_MONTH));
        }

        if ((date.get(Calendar.HOUR_OF_DAY)) < 10) {
            hour = "0" + Integer.toString(date.get(Calendar.HOUR_OF_DAY));
        } else {
            hour = Integer.toString(date.get(Calendar.HOUR_OF_DAY));
        }

        if ((date.get(Calendar.MINUTE)) < 10) {
            minute = "0" + Integer.toString(date.get(Calendar.MINUTE));
        } else {
            minute = Integer.toString(date.get(Calendar.MINUTE));
        }

        if ((date.get(Calendar.SECOND)) < 10) {
            second = "0" + Integer.toString(date.get(Calendar.SECOND));
        } else {
            second = Integer.toString(date.get(Calendar.SECOND));
        }

        if (format == 1) {
            sb.append(year);
            sb.append(month);
            sb.append(day);
            sb.append(hour);
            sb.append(minute);
            sb.append(second);
        } else if (format == 2) {
            sb.append(day);
            sb.append("/");
            sb.append(month);
            sb.append("/");
            sb.append(year);
            sb.append(" ");
            sb.append(hour);
            sb.append(":");
            sb.append(minute);
            sb.append(":");
            sb.append(second);
        } else if (format == 3) {
            sb.append(year);
            sb.append(month);
            sb.append(day);
        }

        return sb.toString();
    }

    /**
     * Gets formatted eventout record number for logger.
     *
     * @return Formatted eventout record number
     */
    private String getRecordNumber() {
        return "<" + eventID + "> -> ";
    }
}
