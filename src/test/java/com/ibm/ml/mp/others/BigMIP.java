package com.ibm.ml.mp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.WmlCplex;
import ilog.cplex.IloCplex;

public class BigMIP {

    public static void main(String[] args)  {
        try {
            IloCplex cplex = new WmlCplex(
                    Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                    Connector.Runtime.DO_12_10,
                    Connector.TShirtSize.M,
                    1);
            int nx = 2000;
            int ny = 1500;
            int nz = 10;

            IloIntVar[] intVarArray = cplex.intVarArray(nx, 0, 10);
            cplex.minimize(cplex.sum(intVarArray));

            for (int j = 0; j < nz; j++)
                for (int i = 0; i < ny; i++) {
                    cplex.add(cplex.eq(cplex.sum(intVarArray), ny));
                }


            if (cplex.solve()) {
                System.out.println(cplex.getStatus());
            }
        }
        catch (IloException e){
            System.out.println("Error: "+e.getMessage());
        }
    }
}
