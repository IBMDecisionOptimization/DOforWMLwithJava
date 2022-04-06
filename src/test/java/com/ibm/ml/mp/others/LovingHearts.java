package com.ibm.ml.mp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.WmlCplex;
import ilog.cplex.IloCplex;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LovingHearts {


    // the circles are numbered as follows:
    //
    // 1               (1, 1)
    // 2   3         (2, 1)   (2, 2)
    // 4   5   6   (3, 1)   (3, 2)   (3, 3)
    // ....
    static IloCplex buildHearts(int r) throws IloException {
        IloCplex cplex = new WmlCplex(
                Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                Connector.Runtime.DO_20_1,
                Connector.TShirtSize.M,
                1);

        // the dictionary of decision variables, one variable
        // for each circle with i in (1 .. r) as the row and
        // j in (1 .. i) as the position within the row
        Map<Integer, Map<Integer, IloIntVar>> a = new HashMap<>();
        LinkedList<IloIntVar> vars = new LinkedList<>();
        for (int i = 1; i < r + 1; i++) {
            Map<Integer, IloIntVar> b = new HashMap<>();
            for (int j = 1; j < i + 1; j++) {
                IloIntVar v = cplex.boolVar();
                vars.add(v);
                b.put(j, v);
            }
            a.put(i, b);
        }

        // the constraints - enumerate all equilateral triangles
        // and prevent any such triangles being formed by keeping
        // the number of included circles at its vertexes below 3

        // for each row except the last
        for (int i = 1; i < r; i++)
            // for each position in this row
            for (int j = 1; j < i + 1; j++)
                // for each triangle of side length (k) with its upper vertex at
                // (i, j) and its sides parallel to those of the overall shape
                for (int k = 1; k < r - i + 1; k++)
                    // the sets of 3 points at the same distances clockwise along the
                    // sides of these triangles form k equilateral triangles
                    for (int m = 0; m < k; m++) {
                        int ux = i + m;
                        int uy = j;

                        int vx = i + k;
                        int vy = j + m;

                        int wx = i + k - m;
                        int wy = j + k - m;

                        cplex.add(
                                cplex.le(
                                        cplex.sum(
                                                cplex.sum(a.get(ux).get(uy), a.get(vx).get(vy)),
                                                a.get(wx).get(wy)
                                        )
                                        , 2
                                )
                        );
                    }

        IloIntVar[] allVars = new IloIntVar[vars.size()];
        int i = 0;
        for (IloIntVar v : vars) {
            allVars[i] = v;
            i++;
        }

        cplex.add(cplex.maximize(cplex.sum(allVars)));
        return cplex;
    }


    public static void main(String[] args)  {
        try {
            int r = 10;
            IloCplex cplex = buildHearts(r);
            cplex.setParam(IloCplex.Param.TimeLimit, 30 * 60);
            if (cplex.solve()) {
                System.out.println("Solution status: " + cplex.getStatus());
                System.out.println("Objective: " + cplex.getObjValue());
            } else {
                System.out.println("No solution");
            }
        }
        catch (IloException e){
            System.out.println("Error: "+e.getMessage());
        }
    }
}
