package com.netcracker.qubership.vsec;

/**
 * List of error codes to identify which error cases were documented or explained
 */
public enum ErrorCodes {
    ERR001("001"),      // Illegal Application State
    ERR002("002"),      // Can't load properties from properties file
    ERR003("003"),      // No database drive can be found
    ERR004("004"),      // Error while opening websocket connection for Mattermost
    ERR005("005"),      // Error while processing message from Mattermost
    ERR006("006"),      // Some unclear error happened during processing messages from Mattermost
    ERR007("007"),      // SQL Exception happened during working with DB
    ERR008("008"),      // Some error happened when executing active jobs
    ERR009("009"),      // SQL Exception while creating table in database
    ERR010("010"),      // SQL Exception while saving data into databse
    ERR011("011"),      // SQL Exception while executing cleaning up in database
    ERR012("012"),      // xxx
    ERR013("013"),      // xxx
    ERR014("014"),      // xxx
    ERR015("015"),      // xxx
    ERR016("016"),      // xxx
    ;

    private final String code;

    ErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
