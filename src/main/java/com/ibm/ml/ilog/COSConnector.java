package com.ibm.ml.ilog;

import com.ibm.json.java.JSONObject;
import ilog.concert.IloException;

import java.io.IOException;

/*
A Simple connector to IBM Cloud Object Storage
 */
public interface COSConnector extends TokenHandler {
    static COSConnector getConnector(Credentials creds) throws IloException {
        return new com.ibm.ml.ilog.v4.COSConnector(creds);
    }

    /*
    Builds a data reference from an id.
     */
    JSONObject getDataReferences(String id) throws IloException;

    /*
    Method to upload a file on the disk to a COS bucket.
     */
    String putFile(String fileName, String filePath) throws IloException;

    /*
    Method to create a connection asset from COS credentials.
     */
    String getConnection() throws IloException;
    /*
    Download the content of a COS file as a string.
     */
    String getFile(String fileName) throws IloException;
}