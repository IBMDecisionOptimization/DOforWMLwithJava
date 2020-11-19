package com.ibm.ml;

import com.ibm.ml.ilog.Connector;
import com.ibm.ml.ilog.Credentials;
import ilog.concert.IloException;


import java.io.IOException;
import java.util.LinkedList;

public class CleanupWML {
    public static void main(String[] args)  {
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

                            try {
                                if (isCplex) {
                                    String name = Connector.getCplexPrefix() + runtime + "." + size + "." + node;
                                    String id = connector.getDeploymentIdByName(name);
                                    if (id != null) {
                                        connector.deleteDeployment(id);
                                        deployments.add(id);
                                    }
                                }
                                if (isCPO) {
                                    String name = Connector.getCPOPrefix() + runtime + "." + size + "." + node;
                                    String id = connector.getDeploymentIdByName(name);
                                    if (id != null) {
                                        connector.deleteDeployment(id);
                                        deployments.add(id);
                                    }
                                }
                            }catch (Exception e){

                            }
                            finally {
                                connector.end();
                            }
                        }
            System.out.println("");
            for (String id : deployments) {
                System.out.println("Deployment was deleted " + id);
            }
        }
        catch (IloException e){
            System.out.println("Error: "+e.getMessage());
        }
    }
}
