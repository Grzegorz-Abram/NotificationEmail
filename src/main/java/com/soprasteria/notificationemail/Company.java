package com.soprasteria.notificationemail;

import java.util.ArrayList;
/**
 *
 * @author sgacka
 */
public class Company {

    private ArrayList<CompanyRecord> company;
    private final Database db;

    /**
     * Company constructor.
     * @param db Database object
     */
    public Company(Database db) {
        this.company = new ArrayList<CompanyRecord>();
        this.db = db;
    }

    /**
     * Process records in company table
     * @throws Exception
     */
    public void getCompanies() throws Exception {
        company = db.getCompanies();
    }

    /**
     * Gets number of records in company table.
     * @return Number of records
     */
    public int getRecordsCount() {
        return company.size();
    }

    /**
     * Gets company record from list.
     * @param name Name of company
     * @return Company record
     */
    public CompanyRecord getCompanyRecord(String name) {
        for (int i=0; i < company.size(); i++)
        {
            CompanyRecord record = company.get(i);
            
            if (record.getCompany().equals(name))
            {
                return company.get(i);
            }
        }
        
        return null;
    }
}