package com.ibm.ml.cp.unit_tests;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloIntVarArray;
import ilog.concert.cppimpl.IloAlgorithm.Status;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestFacility extends TestCase {

	private int nbLocations;
	private int nbStores;
	private int[] capacity;
	private int[] fixedCost;
	private int[][] cost;

	private IloIntVar[] supplier;
	private IloIntVar[] open;

	private IloIntExpr obj;

	public void setUp() throws IloException {
		obj = null;
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
		String filename = "cpo/facility.data";
		IloCP cp = null;
		try {
			cp = new WmlCP(
					Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
					Connector.Runtime.DO_12_10,
					Connector.TShirtSize.M,
					1);
			int i, j;

			DataReader data = new DataReader(filename);

			nbLocations = data.next();
			nbStores = data.next();
			capacity = new int[nbLocations];
			fixedCost = new int[nbLocations];
			cost = new int[nbStores][];

			for (i = 0; i < nbStores; i++)
				cost[i] = new int[nbLocations];

			for (j = 0; j < nbLocations; j++)
				capacity[j] = data.next();

			for (j = 0; j < nbLocations; j++)
				fixedCost[j] = data.next();

			for (i = 0; i < nbStores; i++)
				for (j = 0; j < nbLocations; j++)
					cost[i][j] = data.next();

			supplier = cp.intVarArray(nbStores, 0, nbLocations - 1);
			open = cp.intVarArray(nbLocations, 0, 1);

			for (i = 0; i < nbStores; i++)
				cp.add(cp.eq(cp.element(open, supplier[i]), 1));

			for (j = 0; j < nbLocations; j++)
				cp.add(cp.le(cp.count(supplier, j), capacity[j]));

			obj = cp.scalProd(open, fixedCost);
			for (i = 0; i < nbStores; i++)
				obj = cp.sum(obj, cp.element(cost[i], supplier[i]));

			cp.add(cp.minimize(obj));
		} catch (IloException | IOException e) {
			e.printStackTrace();
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_Facility() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}

		try {
			assertTrue(1383 == (int) cp.getObjValue());
			Status status = cp.getStatus();
			assertEquals(status, Status.Optimal);

			List<Integer> openFacilities = new ArrayList<Integer>();
			for (int j = 0; j < nbLocations; j++) {
				if (cp.getValue(open[j]) == 1) {
					openFacilities.add(j);
					System.out.print("Facility " + j + " is open, it serves stores ");
					for (int i = 0; i < nbStores; i++) {
						if (cp.getValue(supplier[i]) == j)
							System.out.print(i + " ");
					}
					System.out.println();
				}
			}
			assertTrue(openFacilities.contains(0));
			assertTrue(openFacilities.contains(1));
			assertFalse(openFacilities.contains(2));
			assertTrue(openFacilities.contains(3));

			double[]	supplier_values = new double[supplier.length];
			cp.getValues(supplier, supplier_values);
			System.out.println("supplier_values: " + Arrays.toString(supplier_values));
			for (int i = 0; i < supplier_values.length; i++) {
				assertEquals(cp.getValue(supplier[i]), supplier_values[i]);
			}


			IloIntVarArray allIntVars = cp.getCPImpl().getAllIloIntVars();
			double[] allIntValues = new double[allIntVars.getSize()];
			cp.getValues(allIntVars, allIntValues);
			System.out.println("allIntValues: " + Arrays.toString(allIntValues));
			for (int i = 0; i < allIntVars.getSize(); i++) {
				assertEquals(cp.getValue(allIntVars.getIntVar(i)), allIntValues[i]);
			}

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure while browsing model solution with getters");
		}

		try {
			int value = (int) cp.getValue(obj);
			fail("getValue(...) not be supported by design (as of october 2020)");
			
			assertEquals(value, 1383);

		} catch (Exception e) {
			e.printStackTrace();
			assumeTrue("'Expected' failure while accessing value of expression", true);
		}
	}

}
