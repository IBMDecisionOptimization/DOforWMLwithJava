package com.ibm.ml.cp.unit_tests;

import java.io.IOException;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloSolution;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestPlantLocation extends TestCase {

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

	public IloCP build_model() {

		IloCP cp = null;
		try {
			cp = new WmlCP(
					Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
					Connector.Runtime.DO_20_1,
					Connector.TShirtSize.M,
			        1);

			DataReader data = new DataReader("cpo/plant_location.data");
			int nbCustomer = data.next();
			int nbLocation = data.next();

			int[][] cost = new int[nbCustomer][];
			for (int c = 0; c < nbCustomer; c++) {
				cost[c] = new int[nbLocation];
				for (int w = 0; w < nbLocation; w++) {
					cost[c][w] = data.next();
				}
			}
			int[] demand = new int[nbCustomer];
			int totalDemand = 0;
			for (int c = 0; c < nbCustomer; c++) {
				demand[c] = data.next();
				totalDemand += demand[c];
			}
			int[] fixedCost = new int[nbLocation];
			for (int w = 0; w < nbLocation; w++)
				fixedCost[w] = data.next();
			int[] capacity = new int[nbLocation];
			for (int w = 0; w < nbLocation; w++)
				capacity[w] = data.next();

			IloIntVar cust[] = new IloIntVar[nbCustomer];
			for (int c = 0; c < nbCustomer; c++)
				cust[c] = cp.intVar(0, nbLocation - 1);

			IloIntVar[] open = new IloIntVar[nbLocation];
			IloIntVar[] load = new IloIntVar[nbLocation];
			for (int w = 0; w < nbLocation; w++) {
				open[w] = cp.intVar(0, 1);
				load[w] = cp.intVar(0, capacity[w]);
				cp.add(cp.eq(open[w], cp.gt(load[w], 0)));
			}
			cp.add(cp.pack(load, cust, demand));

			IloNumExpr obj = cp.scalProd(fixedCost, open);
			for (int c = 0; c < nbCustomer; c++) {
				obj = cp.sum(obj, cp.element(cost[c], cust[c]));
			}
			cp.add(cp.minimize(obj));

			cp.addKPI(cp.quot(totalDemand, cp.scalProd(open, capacity)), "Mean occupancy");
			IloNumExpr[] usage = new IloNumExpr[nbLocation];
			for (int w = 0; w < nbLocation; w++)
				usage[w] = cp.sum(cp.quot(load[w], capacity[w]), cp.diff(1, open[w]));
			cp.addKPI(cp.min(usage), "Min occupancy");

			int[] custValues = { 19, 0, 11, 8, 29, 9, 29, 28, 17, 15, 7, 9, 18, 15, 1, 17, 25, 18, 17, 27, 22, 1, 26, 3,
			        22, 2, 20, 27, 2, 16, 1, 16, 12, 28, 19, 2, 20, 14, 13, 27, 3, 9, 18, 0, 13, 19, 27, 14, 12, 1, 15,
			        14, 17, 0, 7, 12, 11, 0, 25, 16, 22, 13, 16, 8, 18, 27, 19, 23, 26, 13, 11, 11, 19, 22, 28, 26, 23,
			        3, 18, 23, 26, 14, 29, 18, 9, 7, 12, 27, 8, 20 };

			IloSolution sol = cp.solution();
			for (int c = 0; c < nbCustomer; c++) {
				sol.setValue(cust[c], custValues[c]);
			}

			cp.setStartingPoint(sol);
			cp.setParameter(IloCP.DoubleParam.TimeLimit, 10);
			cp.setParameter(IloCP.IntParam.LogPeriod, 10000);
		} catch (IloException | IOException e) {
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_PlantLocation() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);

			assertTrue(71000 >= (int) cp.getObjValue());

			double kpi1Value = cp.getKPIValue("Mean occupancy");
			double kpi2Value = cp.getKPIValue("Min occupancy");
			assertTrue(0.9 <= kpi1Value);
			assertTrue(0.8 <= kpi2Value);

			// assertEquals(70908, (int) cp.getObjValue());

			// assertEquals(0.986926, kpi1Value, 1e-3);
			// assertEquals(0.877551, kpi2Value, 1e-3);

			assertEquals(38456, cp.getObjBound(), 1e-3);
			assertEquals(38456, cp.getObjBounds()[0], 1e-3);

			assertTrue(0.5 >= cp.getObjGap());
			// assertEquals(0.457663, cp.getObjGap(), 1e-3);

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}
	}

}
