package com.ibm.ml;

import com.ibm.ml.ilog.Connector;
import com.ibm.ml.ilog.Credentials;
import ilog.concert.IloException;

import java.io.IOException;


public class BrowseWML {
    public static void main(String[] args)  {
        Connector connector = null;
        try {
            Credentials credentials = Credentials.getCredentials("wml.public.conf");
            connector = Connector.getConnector(credentials);
            connector.initToken();

            System.out.println("Browsing WML");
            System.out.println(" Instances = " + connector.getInstances().toString());
            System.out.println(" Spaces = " + connector.getDeploymentSpaces().toString());
            System.out.println(" Software Specifications = " + connector.getSoftwareSpecifications().toString());

            System.out.println("Browsing WML by space id");
            System.out.println(" Deployments " + connector.getDeployments().toString());
            System.out.println(" Models " + connector.getModels().toString());
            System.out.println(" Jobs " + connector.getDeploymentJobs().toString());
            System.out.println(" Storage " + connector.getStorage().toString());
        }
        catch (IloException e){
            System.out.println("Something bad happened "+e.getMessage());
        }
        finally {
            connector.end();
        }
    }
}
