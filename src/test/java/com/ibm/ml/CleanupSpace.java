package com.ibm.ml;

import com.ibm.ml.ilog.Connector;
import com.ibm.ml.ilog.Credentials;
import ilog.concert.IloException;


import java.io.IOException;

public class CleanupSpace {
    public static void main(String[] args)  {
        Connector connector = null;
        try {
            Credentials credentials = Credentials.getCredentials("wml.public.conf");

            connector = Connector.getConnector(credentials);
            connector.initToken();
            connector.cleanSpace();
        }
        catch (IloException e) {
            System.out.println("Error: " + e.getMessage());
        }
        finally {
            connector.end();
        }
    }
}
