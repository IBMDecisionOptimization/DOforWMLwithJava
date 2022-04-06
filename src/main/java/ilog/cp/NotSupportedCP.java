/* --------------------------------------------------------------------------
   Source file provided under Apache License, Version 2.0, January 2004,
   http://www.apache.org/licenses/
   (c) Copyright IBM Corp. 2016

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
   --------------------------------------------------------------------------
 */
// This class is in package ilog.cplex so that we can call some undocumented functions
package ilog.cp;

import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ilog.concert.IloConstraint;
import ilog.concert.IloCumulFunctionExpr;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloSolution;
import ilog.concert.cppimpl.IloConstraintArray;

/**
 * Base class for external solves.
 */
abstract class NotSupportedCP extends IloCP {
    private static final Logger logger = LogManager.getLogger();
    protected String notSupportedError = "Not supported by the CPO WML Connector";


    public NotSupportedCP() {
        super();
    }


    /************************************************************************
     * Functions for managing: Explanations
     ************************************************************************/
    @Override
    public void clearExplanations() throws IloException {
        logger.error(notSupportedError + " clearExplanations");
        throw new RuntimeException(notSupportedError);    }

    @Override
    public void explainFailure(int failTag) throws IloException {
        logger.error(notSupportedError + " explainFailure");
        throw new RuntimeException(notSupportedError);    }

    @Override
    public void explainFailure(int[] failTagArray) throws IloException {
        logger.error(notSupportedError + " explainFailure");
        throw new RuntimeException(notSupportedError);
    }


    /************************************************************************
     * Functions for retrieving search information
     ************************************************************************/
	@Override
	public double getInfo(DoubleInfo which) throws IloException {
        logger.error(notSupportedError + " getInfo");
        throw new RuntimeException(notSupportedError);
	}

	@Override
	public int getInfo(IntInfo which) throws IloException {
        logger.error(notSupportedError + " getInfo");
        throw new RuntimeException(notSupportedError);
	}


	/************************************************************************
     * Functions for getting values on Expressions
     ************************************************************************/
    @Override
    public double getValue(IloIntExpr i) {
        logger.error(notSupportedError + " getValue");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public double getValue(IloNumExpr num) {
        logger.error(notSupportedError + " getValue");
        throw new RuntimeException(notSupportedError);
    }

	@Override
	public double getIncumbentValue(IloNumExpr expr) throws IloException {
        logger.error(notSupportedError + " getIncumbentValue");
        throw new RuntimeException(notSupportedError);
	}

	@Override
	public double getIncumbentValue(String exprName) throws IloException {
        logger.error(notSupportedError + " getIncumbentValue");
        throw new RuntimeException(notSupportedError);
	}


	/************************************************************************
     * Functions for getting segment values for Cumulative Functions
     ************************************************************************/
	@Override
	public int getNumberOfSegments(IloCumulFunctionExpr f) throws IloException {
        logger.error(notSupportedError + " getNumberOfSegments");
        throw new RuntimeException(notSupportedError);
	}

	@Override
    public int getSegmentStart(IloCumulFunctionExpr f, int i) throws IloException {
        logger.error(notSupportedError + " getSegmentStart");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public int getSegmentEnd(IloCumulFunctionExpr f, int i) throws IloException {
        logger.error(notSupportedError + " getSegmentEnd");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public int getSegmentValue(IloCumulFunctionExpr f, int i) throws IloException {
        logger.error(notSupportedError + " getSegmentValue");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public int getValue(IloCumulFunctionExpr f, int t) throws IloException {
        logger.error(notSupportedError + " getValue");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public boolean isFixed(IloCumulFunctionExpr f) throws IloException {
        logger.error(notSupportedError + " isFixed");
        throw new RuntimeException(notSupportedError);
    }


    /************************************************************************
     * Functions for managing: Callbacks
     ************************************************************************/
    @Override
    public void addCallback(IloCP.Callback cb) throws IloException {
        logger.error(notSupportedError + " addCallback");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public void removeCallback(IloCP.Callback cb) throws IloException {
        logger.error(notSupportedError + " removeCallback");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public void removeAllCallbacks() throws IloException {
        logger.error(notSupportedError + " removeAllCallbacks");
        throw new RuntimeException(notSupportedError);
    }

    
    /************************************************************************
     * Functions for managing: Conflicts
     ************************************************************************/
    @Override
    public boolean refineConflict(IloConstraint[] csts) throws IloException {
        logger.error(notSupportedError + " refineConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public boolean refineConflict(IloConstraint[] csts, double[] prefs) throws IloException {
        logger.error(notSupportedError + " refineConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public boolean refineConflict(IloSolution sol) {
        logger.error(notSupportedError + " refineConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public boolean refineConflict(IloConstraintArray csts) throws IloException {
        logger.error(notSupportedError + " refineConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public boolean refineConflict(IloConstraintArray csts, ilog.concert.IloNumArray prefs) throws IloException {
        logger.error(notSupportedError + " refineConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public IloCP.ConflictStatus getConflict(IloConstraint cst) throws IloException {
        logger.error(notSupportedError + " getConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public IloCP.ConflictStatus getConflict(IloNumVar var) throws IloException {
        logger.error(notSupportedError + " getConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public IloCP.ConflictStatus getConflict(IloIntervalVar var) throws IloException {
        logger.error(notSupportedError + " getConflict");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public void exportConflict(OutputStream s) throws IloException {
        logger.error(notSupportedError + " exportConflict");
        throw new RuntimeException(notSupportedError);
    }


    /************************************************************************
     * Functions for controlling search and performing statistics
     ************************************************************************/
    @Override
    public void runSeeds(int n) throws IloException {
        logger.error(notSupportedError + " runSeeds");
        throw new RuntimeException(notSupportedError);
    }

    @Override
    public void runSeeds() throws IloException {
        logger.error(notSupportedError + " runSeeds");
        throw new RuntimeException(notSupportedError);
    }

	@Override
	public void startNewSearch() throws IloException {
        logger.error(notSupportedError + " startNewSearch");
        throw new RuntimeException(notSupportedError);
	}

	@Override
	public boolean next() throws IloException {
        logger.error(notSupportedError + " next");
        throw new RuntimeException(notSupportedError);
	}

}
