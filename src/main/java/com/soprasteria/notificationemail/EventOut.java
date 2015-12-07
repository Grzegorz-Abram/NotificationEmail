package com.soprasteria.notificationemail;

import java.util.ArrayList;

/**
 * Class that represents Eventout queue in HPSM.
 * @author sgacka
 */
public class EventOut {

    private ArrayList<EventOutRecord> eventout;
    private final Database db;

    /**
     * EventOut constructor.
     * @param db Database object
     */
    public EventOut(Database db) {
        this.eventout = new ArrayList<EventOutRecord>();
        this.db = db;
    }

    /**
     * Process records in eventout queue - page event type.
     * @throws Exception
     */
    public void getEventOut() throws Exception {
        eventout = db.getEventOutRecords();
    }

    /**
     * Gets number of records in eventout.
     * @return Number of records
     */
    public int getRecordsCount() {
        return eventout.size();
    }

    /**
     * Gets eventout record from list.
     * @param position Position on list
     * @return Eventout record
     */
    public EventOutRecord getEventOutRecord(int position) {
        return eventout.get(position);
    }
}
