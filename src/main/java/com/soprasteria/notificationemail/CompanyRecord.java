package com.soprasteria.notificationemail;

/**
 * Class that represents CompanyRecord queue in HPSM.
 *
 * @author sgacka
 */
public class CompanyRecord {

    private final String company;
    private String senderEmail = null;
    private String senderName = null;

    /**
     *
     * @param company
     * @param senderEmail
     * @param senderName
     */
    public CompanyRecord(String company, String senderName, String senderEmail) {
        this.company = company;

        if (senderName != null) {
            this.senderName = senderName.trim().replace(Configuration.NBSP, "");
        }
        if (senderEmail != null) {
            this.senderEmail = senderEmail.trim().replace(Configuration.NBSP, "");
        }
    }

    public String getCompany() {
        return company;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }
}
