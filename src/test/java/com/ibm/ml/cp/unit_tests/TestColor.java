package com.ibm.ml.cp.unit_tests;

import java.io.IOException;

import com.ibm.ml.ilog.Credentials;

import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestColor extends TestCase {

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

	public static String[] Names = { "blue", "white", "yellow", "green" };
	
	IloIntVar Belgium, Denmark, France, Germany, Luxembourg, Netherlands;

	public IloCP build_model() {
		IloCP cp = null;
		try {
			// IloCP cp = new IloCP();
			cp = new WmlCP(
					Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
					Connector.Runtime.DO_12_10, Connector.TShirtSize.M, 1);
			Belgium = cp.intVar(0, 3);
			Denmark = cp.intVar(0, 3);
			France = cp.intVar(0, 3);
			Germany = cp.intVar(0, 3);
			Luxembourg = cp.intVar(0, 3);
			Netherlands = cp.intVar(0, 3);

			cp.add(cp.neq(Belgium, France));
			cp.add(cp.neq(Belgium, Germany));
			cp.add(cp.neq(Belgium, Netherlands));
			cp.add(cp.neq(Belgium, Luxembourg));
			cp.add(cp.neq(Denmark, Germany));
			cp.add(cp.neq(France, Germany));
			cp.add(cp.neq(France, Luxembourg));
			cp.add(cp.neq(Germany, Luxembourg));
			cp.add(cp.neq(Germany, Netherlands));

		} catch (IloException e) {
			System.err.println("Error " + e);
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_Color() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);
			if (res) {
				assertEquals("blue", Names[(int)cp.getValue(Belgium)]);
				assertEquals("blue", Names[cp.getIntValue(Belgium)]);
				assertEquals("blue", Names[(int)cp.getValue(Denmark)]);
				assertEquals("green", Names[(int)cp.getValue(France)]);
				assertEquals("white", Names[(int)cp.getValue(Germany)]);
				assertEquals("yellow", Names[(int)cp.getValue(Luxembourg)]);
				assertEquals("green", Names[(int)cp.getValue(Netherlands)]);
			}
		} catch (IloException e) {
			fail("Failure during model generation");
		}
	}

}
