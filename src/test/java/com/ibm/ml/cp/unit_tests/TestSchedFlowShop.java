package com.ibm.ml.cp.unit_tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloObjective;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestSchedFlowShop extends TestCase {

	public void setUp() throws IloException {
	}

	/**
	 * Tear-down testing environment
	 */

	public void tearDown() {
	}

	/*----------------------------------------------------------------------------
	Test methods
	----------------------------------------------------------------------------*/

    static class IntervalVarList extends ArrayList<IloIntervalVar> {
		private static final long serialVersionUID = 1L;

		public IloIntervalVar[] toArray() {
            return (IloIntervalVar[]) this.toArray(new IloIntervalVar[this.size()]);
        }
    }

    static IloIntExpr[] arrayFromList(List<IloIntExpr> list) {
        return (IloIntExpr[]) list.toArray(new IloIntExpr[list.size()]);
    }

    public IloCP build_model() {

		IloCP cp = null;
		try {
	        String filename = "cpo/flowshop_default.data";
	        int failLimit = 10000;
	        int nbJobs, nbMachines;

	        cp = new WmlCP(
					Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
					Connector.Runtime.DO_12_10,
					Connector.TShirtSize.M,
					1);

	        DataReader data = new DataReader(filename);

            nbJobs = data.next();
            nbMachines = data.next();
            List<IloIntExpr> ends = new ArrayList<IloIntExpr>();
            IntervalVarList[] machines = new IntervalVarList[nbMachines];
            for (int j = 0; j < nbMachines; j++)
                machines[j] = new IntervalVarList();

            for (int i = 0; i < nbJobs; i++) {
                IloIntervalVar prec = cp.intervalVar();
                for (int j = 0; j < nbMachines; j++) {
                    int d = data.next();
                    IloIntervalVar ti = cp.intervalVar(d);
                    machines[j].add(ti);
                    if (j > 0) {
                        cp.add(cp.endBeforeStart(prec, ti));
                    }
                    prec = ti;
                }
                ends.add(cp.endOf(prec));
            }
            for (int j = 0; j < nbMachines; j++)
                cp.add(cp.noOverlap(machines[j].toArray()));

            IloObjective objective = cp.minimize(cp.max(arrayFromList(ends)));
            cp.add(objective);

            cp.setParameter(IloCP.IntParam.FailLimit, failLimit);
		} catch (IloException | IOException e) {
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_SchedFlowShop() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);

            System.out.println("Makespan \t: " + cp.getObjValue());

            assertTrue(1274 >= (int) cp.getObjValue());


		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}
	}

}
