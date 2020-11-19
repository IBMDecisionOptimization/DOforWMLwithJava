package com.ibm.ml.cp.unit_tests;

import java.io.IOException;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloCumulFunctionExpr;
import ilog.concert.IloException;
import ilog.concert.IloIntervalVar;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import ilog.cp.IloSearchPhase;
import junit.framework.TestCase;

public class TestSchedSquare extends TestCase {

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

    private static int nbSquares = 21;
    private IloIntervalVar[] x;
    private IloIntervalVar[] y;

	public IloCP build_model() {
		IloCP cp = null;
		try {
            cp = new WmlCP(
                    Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                    Connector.Runtime.DO_12_10,
                    Connector.TShirtSize.M,
                    1);

            int sizeSquare = 112;
            int[] size = {50, 42, 37, 35, 33, 29, 27, 25, 24, 19, 18, 17, 16, 15, 11, 9, 8, 7, 6, 4, 2};
            x = new IloIntervalVar[nbSquares];
            y = new IloIntervalVar[nbSquares];
            IloCumulFunctionExpr rx = cp.cumulFunctionExpr();
            IloCumulFunctionExpr ry = cp.cumulFunctionExpr();

            for (int i = 0; i < nbSquares; ++i) {
                x[i] = cp.intervalVar(size[i], "X" + i);
                x[i].setEndMax(sizeSquare);
                y[i] = cp.intervalVar(size[i], "Y" + i);
                y[i].setEndMax(sizeSquare);
                rx = cp.sum(rx, cp.pulse(x[i], size[i]));
                ry = cp.sum(ry, cp.pulse(y[i], size[i]));

                for (int j = 0; j < i; ++j) {
                    cp.add(cp.or(cp.le(cp.endOf(x[i]), cp.startOf(x[j])),
                            cp.or(cp.le(cp.endOf(x[j]), cp.startOf(x[i])),
                                    cp.or(cp.le(cp.endOf(y[i]), cp.startOf(y[j])),
                                            cp.le(cp.endOf(y[j]), cp.startOf(y[i]))))));
                }
            }
            cp.add(cp.alwaysIn(rx, 0, sizeSquare, sizeSquare, sizeSquare));
            cp.add(cp.alwaysIn(ry, 0, sizeSquare, sizeSquare, sizeSquare));

            IloSearchPhase[] phases = new IloSearchPhase[2];
            phases[0] = cp.searchPhase(x);
            phases[1] = cp.searchPhase(y);

            cp.setSearchPhases(phases);

		} catch (IloException e) {
			System.err.println("Error " + e);
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_SchedSquare() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);

			for (int i = 0; i < x.length; i++) {
				assertEquals(cp.getSize(x[i]), cp.getEnd(x[i]) - cp.getStart(x[i]));
			}
			
			assertEquals(0, cp.getStart(x[0]));
			assertEquals(50, cp.getEnd(x[0]));
			assertEquals(0, cp.getStart(y[0]));
			assertEquals(50, cp.getEnd(y[0]));
		} catch (IloException e) {
			fail("Failure during model generation");
		}
	}

}
