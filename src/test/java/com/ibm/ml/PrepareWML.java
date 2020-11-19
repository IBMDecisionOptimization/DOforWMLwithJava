package com.ibm.ml;

import com.ibm.ml.ilog.Connector;
import com.ibm.ml.ilog.Credentials;
import ilog.concert.IloException;


import java.io.IOException;
import java.util.LinkedList;

public class PrepareWML {
    public static void main(String[] args) {
        try {
            Credentials[] credentials = new Credentials[]{
                    Credentials.getCredentials("wml.public.conf")
            };
            Connector.Runtime[] runtimes = new Connector.Runtime[]{
                    Connector.Runtime.DO_12_10,
                    Connector.Runtime.DO_12_9
            };
            Connector.TShirtSize[] sizes = new Connector.TShirtSize[]{
                    Connector.TShirtSize.M
            };
            int[] nodes = new int[]{1};

            boolean isCplex = true;
            boolean isCPO = false;
            LinkedList<String> deployments = new LinkedList<>();

            for (Credentials creds : credentials)
                for (Connector.Runtime runtime : runtimes)
                    for (Connector.TShirtSize size : sizes)
                        for (int node : nodes) {
                            Connector connector = Connector.getConnector(
                                    creds,
                                    runtime,
                                    size,
                                    node);
                            connector.initToken();

                            if (isCplex) {
                                String name = Connector.getCplexPrefix() + runtime + "." + size + "." + node;
                                System.out.println("Looking for " + name);
                                deployments.add(connector.getOrMakeDeployment(name, true));
                            }
                            if (isCPO) {
                                String name = Connector.getCPOPrefix() + runtime + "." + size + "." + node;
                                System.out.println("Looking for " + name);
                                deployments.add(connector.getOrMakeDeployment(name, false));
                            }
                            connector.end();
                        }
            System.out.println("");
            for (String id : deployments) {
                System.out.println("Deployment is " + id);
            }
        }
        catch (IloException e){
            System.out.println("Error "+e.getMessage());
        }
    }
}
