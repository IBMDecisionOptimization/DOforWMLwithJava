package com.ibm.ml.cp.unit_tests;

import java.io.IOException;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;

import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestPPP extends TestCase {

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
	//
	// Data
	//
	private static final int numBoats = 42;
	private static final int numPeriods = 6;

	private static final int[] boatSize = { 7, 8, 12, 12, 12, 12, 12, 10, 10, 10, 10, 10, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8,
	        7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 9, 2, 3, 4 };
	private static final int[] crewSize = { 2, 2, 2, 2, 4, 4, 4, 1, 2, 2, 2, 3, 4, 2, 3, 6, 2, 2, 4, 2, 4, 5, 4, 4, 2,
	        2, 4, 5, 2, 4, 2, 2, 2, 2, 2, 2, 4, 5, 7, 2, 3, 4 };

	private IloIntVar numHosts;
	private IloIntVar[] host;
	private IloIntVar[][] visits;

	//
	// Matrix operations
	//

	public static IloIntVar[][] Transpose(IloIntVar[][] x) {
		int n = x[0].length;
		int m = x.length;
		IloIntVar[][] y = new IloIntVar[n][];
		for (int i = 0; i < n; i++) {
			y[i] = new IloIntVar[m];
			for (int j = 0; j < m; j++)
				y[i][j] = x[j][i];
		}
		return y;
	}

	public IloCP build_model() {
		IloCP cp = null;
		try {
			cp = new WmlCP(
					Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
					Connector.Runtime.DO_12_10,
					Connector.TShirtSize.M,
			        1);

			//
			// Variables
			//

			// Host boat choice
			host = cp.intVarArray(numBoats, 0, 1, "host");

			// Who is where each time period (time- and boat-based views)
			IloIntVar[][] timePeriod = new IloIntVar[numPeriods][];
			for (int i = 0; i < numPeriods; i++)
				timePeriod[i] = cp.intVarArray(numBoats, 0, numBoats - 1, cp.arrayEltName("timePeriod", i));
			visits = Transpose(timePeriod);

			//
			// Objective
			//
			numHosts = cp.intVar(numPeriods, numBoats);
			cp.add(cp.eq(numHosts, cp.sum(host)));
			cp.add(cp.minimize(numHosts));

			//
			// Constraints
			//

			// Stay in my boat (host) or only visit other boats (guest)
			for (int i = 0; i < numBoats; i++)
				cp.add(cp.eq(cp.count(visits[i], i), cp.prod(host[i], numPeriods)));

			// Capacity constraints: only hosts have capacity
			for (int p = 0; p < numPeriods; p++) {
				IloIntVar[] load = new IloIntVar[numBoats];
				for (int j = 0; j < numBoats; j++) {
					load[j] = cp.intVar(0, boatSize[j]);
					cp.add(cp.le(load[j], cp.prod(host[j], boatSize[j])));
				}
				cp.add(cp.pack(load, timePeriod[p], crewSize, numHosts));
			}

			// No two crews meet more than once
			for (int i = 0; i < numBoats; i++) {
				for (int j = i + 1; j < numBoats; j++) {
					IloIntExpr timesMet = cp.constant(0);
					for (int p = 0; p < numPeriods; p++)
						timesMet = cp.sum(timesMet, cp.eq(visits[i][p], visits[j][p]));
					cp.add(cp.le(timesMet, 1));
				}
			}

			// Host and guest boat constraints: given in problem spec
			cp.add(cp.eq(host[0], 1));
			cp.add(cp.eq(host[1], 1));
			cp.add(cp.eq(host[2], 1));
			cp.add(cp.eq(host[39], 0));
			cp.add(cp.eq(host[40], 0));
			cp.add(cp.eq(host[41], 0));
		} catch (IloException e) {
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_PPP() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);

			assertEquals(13, (int) cp.getValue(numHosts));

			// Can't check details of solution as it can varies from one call to another... (several optimal solutions...)
//			assertEquals(1, (int) cp.getValue(host[0]));
//			assertEquals(1, (int) cp.getValue(host[9]));
//			assertEquals(0, (int) cp.getValue(host[10]));
//			assertEquals(1, (int) cp.getValue(host[11]));
//			assertEquals(0, (int) cp.getValue(host[12]));

//			assertEquals(0, (int) cp.getValue(visits[0][0]));
//			assertEquals(3, (int) cp.getValue(visits[10][0]));
//			assertEquals(38, (int) cp.getValue(visits[10][2]));

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}
	}

}
