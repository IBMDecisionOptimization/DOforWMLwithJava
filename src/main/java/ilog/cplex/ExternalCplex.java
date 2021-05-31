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
package ilog.cplex;

import java.io.*;
import java.util.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import ilog.concert.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


abstract class ExternalCplex extends NotSupportedCplex {
    private static final Logger logger = LogManager.getLogger();

    private static final long serialVersionUID = 1;

    private final java.util.LinkedList<IloNumVar> _variables = new java.util.LinkedList<>();

    @Override
    public void end(){
        _variables.clear();
        super.end();
    }

    public ExternalCplex() throws IloException {
        super();
    }

    private void addVariable(HashMap<String, IloNumVar> name2var, HashMap<IloNumVar, String> oldNames, IloNumVar v) throws IloException {
        String name = v.getName();

        if (!oldNames.containsKey(v)) {
            oldNames.put(v, name); // even if name == null!
            name = String.format("vv%x", oldNames.size());
            v.setName(name);
            name2var.put(name, v);
        }
    }

    private void addRange(HashMap<String, IloRange> name2rng, HashMap<IloRange, String> oldNames, IloRange v) throws IloException {
        String name = v.getName();
        if (!oldNames.containsKey(v)) {
            oldNames.put(v, name); // even if name == null!
            name = String.format("cc%x", oldNames.size());
            v.setName(name);
            name2rng.put(name, v);
        }
    }

    /**
     * Solution information.
     * Instances of this class contain solution information that can be obtained from a CPLEX <code>.sol</code> file.
     *
     */
    protected static class Solution {

        private String solution = null;

        boolean feasible = false;
        /**
         * Map variable names to values.
         */
        public HashMap<String, Double> name2val = new HashMap<>();
        public HashMap<String, Double> name2ReducedCost = new HashMap<>();
        /**
         * Map variable objects to values.
         */
        public HashMap<IloNumVar, Double> var2val = new HashMap<>();
        public HashMap<IloNumVar, Double> var2ReducedCost = new HashMap<>();
        /**
         * Map range names to values.
         */
        public HashMap<String, Double> name2dual = new HashMap<>();
        /**
         * Map range objects to values.
         */
        public HashMap<IloRange, Double> rng2dual = new HashMap<>();
        /**
         * Map range names to values.
         */
        public HashMap<String, Double> name2slack = new HashMap<>();
        /**
         * Map range objects to values.
         */
        public HashMap<IloRange, Double> rng2slack = new HashMap<>();
        /**
         * Objective value of solution.
         */
        public double objective = Double.NaN;
        /**
         * CPLEX status.
         */
        public int status;
        /**
         * Primal feasible?
         */
        public boolean pfeas = false;
        /**
         * Dual feasible?
         */
        public boolean dfeas = false;

        public Solution(int status) {
            this.status = status;
        }

        public Solution(File solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IloException, IOException {
            this(-1);
            parse(solutionXml, knownVariables, knownConstraints);
        }

        public boolean hasSolution() {
            return solution != null;
        }

        public String getSolution() {
            return solution;
        }

        public void reset() {
            solution = null;
            feasible = false;
            name2val = new HashMap<>();
            var2val = new HashMap<>();
            name2ReducedCost = new HashMap<>();
            name2dual = new HashMap<>();
            rng2dual = new HashMap<>();
            name2slack = new HashMap<>();
            rng2slack = new HashMap<>();
            objective = Double.NaN;
            status = -1;
            pfeas = false;
            dfeas = false;
        }

        /**
         * Parse a CPLEX <code>.sol</code> file.
         * See {@link #parse(InputStream, Set, Set)} for details.
         */
        public void parse(File solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IOException, IloException {
                reset();
                try (FileInputStream fis = new FileInputStream(solutionXml)) {
                    parse(fis, knownVariables, knownConstraints);
                }
                try (FileInputStream fis = new FileInputStream(solutionXml)) {
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    // StandardCharsets.UTF_8.name() > JDK 7
                    solution = result.toString("UTF-8");
                }
        }

        private enum ParserState {
            INITIAL, SOLUTION, HEADER, QUALITY, VARIABLES, LINEAR_CONSTRAINTS,
            /**
             * Unknown children of a <CPLEXSolution> element.
             */
            UNKNOWN,
            FINISHED
        }

        private static String[] getAttributes(XMLStreamReader reader, String... attrs) {
            final String[] ret = new String[attrs.length];
            final Map<String, String> attrMap = new HashMap<>();
            for (final String s : attrs)
                attrMap.put(s, null);
            final int nattrs = reader.getAttributeCount();
            for (int i = 0; i < nattrs; ++i) {
                final String name = reader.getAttributeLocalName(i);
                if (attrMap.containsKey(name))
                    attrMap.put(name, reader.getAttributeValue(i));
            }
            for (int i = 0; i < attrs.length; ++i)
                ret[i] = attrMap.get(attrs[i]);
            return ret;
        }

        /**
         * Parse a CPLEX <code>.sol</code> file.
         *
         * @param solutionXml    The CPLEX <code>.sol</code> file to parse.
         * @param knownVariables The names of the variables for which values should be extracted from <code>solutionXml</code>.
         * @throws IloException If an input/output error occurs or mandatory solution information is missing.
         */
        private void parse(InputStream solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IOException, IloException {
            reset();

            boolean ok = false;
            try {
                final String MALFORMED_XML = "Malformed XML";
                final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(solutionXml);
                final Vector<String> unknownStack = new Vector<>(4); /* Parsing anything but variables, header, quality
                 * in a CPLEXSolution element. */
                ParserState state = ParserState.INITIAL;
                int solnum = -1; // Solution number for error messages.
                while (reader.hasNext() && state != ParserState.FINISHED) {
                    String element;
                    String[] attrs;
                    switch (reader.next()) {
                        case XMLStreamConstants.START_DOCUMENT: /* nothing */
                            break;
                        case XMLStreamConstants.START_ELEMENT:
                            element = reader.getLocalName();
                            switch (state) {
                                case INITIAL:
                                    // First element must be <CPLEXSolution>
                                    if (element.equals("CPLEXSolution")) {
                                        ++solnum;
                                        state = ParserState.SOLUTION;
                                        this.feasible = true;
                                    } else
                                        throw new IOException(MALFORMED_XML);
                                    break;
                                case HEADER:
                                    throw new IOException(MALFORMED_XML);
                                case QUALITY:
                                    throw new IOException(MALFORMED_XML);
                                case SOLUTION:
                                    if (element.equals("header")) {
                                        state = ParserState.HEADER;
                                        attrs = getAttributes(reader, "objectiveValue", "solutionStatusValue", "primalFeasible", "dualFeasible");
                                        if (attrs[0] == null)
                                            throw new IOException("No objective for solution " + solnum);
                                        this.objective = Double.parseDouble(attrs[0]);
                                        if (attrs[1] == null)
                                            throw new IOException("No solution status for solution " + solnum);
                                        this.status = Integer.parseInt(attrs[1]);
                                        this.pfeas = attrs[2] != null && Integer.parseInt(attrs[2]) != 0;
                                        this.dfeas = attrs[3] != null && Integer.parseInt(attrs[3]) != 0;
                                    } else if (element.equals("quality")) {
                                        state = ParserState.QUALITY;
                                    } else if (element.equals("variables")) {
                                        state = ParserState.VARIABLES;
                                    } else if (element.equals("linearConstraints")) {
                                        state = ParserState.LINEAR_CONSTRAINTS;
                                    }
                                    else if (element.equals("indicatorConstraints")) {
                                        state = ParserState.LINEAR_CONSTRAINTS;
                                    }
                                    else {
                                        state = ParserState.UNKNOWN;
                                        unknownStack.add(element);
                                    }
                                    break;
                                case VARIABLES:
                                    if (!element.equals("variable"))
                                        throw new IOException(MALFORMED_XML);
                                    attrs = getAttributes(reader, "name", "value", "reducedCost");
                                    if (attrs[0] != null) {
                                        if (attrs[1] == null)
                                            throw new IOException("Variable without value in solution file for solution " + solnum);
                                        if (knownVariables.contains(attrs[0]))
                                            this.name2val.put(attrs[0], new Double(attrs[1]));
                                        if (attrs[2] == null) {
                                            // CAN BE MIP
                                            //ignore it
                                        } else {
                                            if (knownVariables.contains(attrs[0]))
                                                this.name2ReducedCost.put(attrs[0], new Double(attrs[2]));
                                        }
                                    }
                                    break;
                                case LINEAR_CONSTRAINTS:
                                    attrs = getAttributes(reader, "name", "dual", "slack");
                                    if (attrs[0] != null) {
                                        if (attrs[1] == null) {
                                            // CAN BE MIP
                                            //ignore it
                                        } else {
                                            if (knownConstraints.contains(attrs[0]))
                                                this.name2dual.put(attrs[0], new Double(attrs[1]));
                                        }
                                        if (attrs[2] == null) {
                                            // CAN BE MIP
                                            //ignore it
                                        } else {
                                            if (knownConstraints.contains(attrs[0]))
                                                this.name2slack.put(attrs[0], new Double(attrs[2]));
                                        }
                                    }
                                    break;
                                case UNKNOWN:
                                    unknownStack.add(element);
                                    break;
                                case FINISHED:
                                    // this cannot happen
                                    throw new IOException(MALFORMED_XML);
                            }
                            break;
                        case XMLStreamConstants.END_ELEMENT:
                            element = reader.getLocalName();
                            switch (state) {
                                case INITIAL:
                                    // This should not happen since we stop after the first solution
                                    throw new IOException(MALFORMED_XML);
                                case SOLUTION:
                                    if (!element.equals("CPLEXSolution"))
                                        throw new IOException(MALFORMED_XML);
                                    // We only parse the very first solution in the file.
                                    state = ParserState.FINISHED;
                                    break;
                                case HEADER:
                                    if (!element.equals("header"))
                                        throw new IOException(MALFORMED_XML);
                                    state = ParserState.SOLUTION;
                                    break;
                                case QUALITY:
                                    if (!element.equals("quality"))
                                        throw new IOException(MALFORMED_XML);
                                    state = ParserState.SOLUTION;
                                    break;
                                case VARIABLES:
                                    if (element.equals("variable")) { /* nothing */ } else if (element.equals("variables")) {
                                        state = ParserState.SOLUTION;
                                    } else
                                        throw new IOException(MALFORMED_XML);
                                    break;
                                case LINEAR_CONSTRAINTS:
                                    if (element.equals("constraint")) { /* nothing */ }
                                    else if (element.equals("linearConstraints")) {
                                        state = ParserState.SOLUTION;
                                    }
                                    else if (element.equals("indicatorConstraints")) {
                                        state = ParserState.SOLUTION;
                                    } else
                                        throw new IOException(MALFORMED_XML);
                                    break;
                                case UNKNOWN:
                                    if (unknownStack.size() == 0 || !element.equals(unknownStack.lastElement()))
                                        throw new IOException(MALFORMED_XML);
                                    unknownStack.remove(unknownStack.size() - 1);
                                    if (unknownStack.size() == 0)
                                        state = ParserState.SOLUTION;
                                    break;
                                case FINISHED:
                                    // This cannot happen
                                    throw new IOException(MALFORMED_XML);
                            }
                            break;
                        //case XMLStreamConstants.END_DOCUMENT: /* nothing */ break;
                    }
                }
                ok = true;
            } catch (XMLStreamException e) {
                throw new IOException(e);
            } finally {
                if (!ok)
                    reset();
            }
        }

    }

    protected Solution result = null;


    @Override
    public IloSemiContVar semiContVar(double var1, double var3, IloNumVarType var5, String var6) throws IloException {
        IloSemiContVar x = super.semiContVar(var1, var3, var5, var6);
        _variables.add(x);
        return x;
    }
    @Override
    public IloSemiContVar semiContVar(double var1, double var3, IloNumVarType var5) throws IloException {
        IloSemiContVar x = super.semiContVar(var1, var3, var5);
        _variables.add(x);
        return x;
    }

    @Override
    public IloSemiContVar semiContVar(IloColumn var1, double var2, double var4, IloNumVarType var6, String var7) throws IloException {
        IloSemiContVar x = super.semiContVar(var1, var2, var4, var6, var7);
        _variables.add(x);
        return x;
    }


    @Override
    public IloNumVar numVar(double var1, double var3, IloNumVarType var5) throws IloException {
        IloNumVar x = super.numVar(var1, var3, var5);
        _variables.add(x);
        return x;
    }
    @Override
    public IloNumVar numVar(double var1, double var3, IloNumVarType var5, String var6) throws IloException {
        IloNumVar x = super.numVar(var1, var3, var5, var6);
        _variables.add(x);
        return x;
    }
    @Override
    public IloNumVar numVar(IloColumn var1, double var2, double var4, IloNumVarType var6) throws IloException {
        IloNumVar x = super.numVar(var1, var2, var4, var6);
        _variables.add(x);
        return x;
    }
    @Override
    public IloNumVar numVar(IloColumn var1, double var2, double var4, IloNumVarType var6, String var7) throws IloException {
        IloNumVar x = super.numVar(var1, var2, var4, var6, var7);
        _variables.add(x);
        return x;
    }

    private boolean _feasOpt(IloConstraint[] cts, double[] prefs, IloRange[] ranges, double[] rlbs, double[] rubs, IloNumVar[] vars, double[] vlbs, double[] vubs) throws IloException{
        String badCall = "Bad call to internal feasopt";
        Relaxations relaxer = new Relaxations();
        if (cts != null){
            if (prefs == null) throw new IloException(badCall);
            if (prefs.length != cts.length) throw new IloException(badCall);
            for (int i=0; i< cts.length; i++) {
                if (cts[i] instanceof IloRange)
                    relaxer.add(cts[i], prefs[i]);
                else
                    throw new IloException("Only ranges are supported by the feasopt.");
            }
        }
        else{
            if (prefs != null) throw new IloException(badCall);
            if (ranges != null){
                if (rlbs == null) throw new IloException(badCall);
                if (ranges.length != rlbs.length) throw new IloException(badCall);
                if (rubs == null) throw new IloException(badCall);
                if (ranges.length != rubs.length) throw new IloException(badCall);
                for (int i=0; i< ranges.length; i++)
                    relaxer.add(ranges[i], rlbs[i], rubs[i]);
            }
            if (vars != null){
                if (vlbs == null) throw new IloException(badCall);
                if (vars.length != vlbs.length) throw new IloException(badCall);
                if (vubs == null) throw new IloException(badCall);
                if (vars.length != vubs.length) throw new IloException(badCall);
                for (int i=0; i< vars.length; i++)
                    relaxer.add(vars[i], vlbs[i], vubs[i]);
            }
        }

        return process(relaxer, null);
    }

    @Override
    public boolean solve() throws IloException {
       return process(null, null);
    }
    private boolean process(Relaxations relaxer, Conflicts conflicts) throws IloException{
        HashMap<IloNumVar, String> oldVarNames = new HashMap<>(_variables.size());

        HashMap<IloRange, String> oldRngNames = new HashMap<>(this.getNrows());

        try {
            // In order to consume a solution file, _all_ variables must have
            // a name! Go through the model and collect all variables, thereby
            // checking that they have a name and names are unique.
            final HashMap<String, IloNumVar> vars = new HashMap<>(_variables.size());
            final HashMap<String, IloRange> rngs = new HashMap<>();
            long t1 = new Date().getTime();
            for (IloNumVar v: _variables)
                addVariable(vars, oldVarNames, v);
            for (Iterator<?> it = iterator(); it.hasNext(); /* nothing */) {
                final Object o = it.next();
                if (o instanceof IloLPMatrix) {
// ignore
                    IloLPMatrix matrix = (IloLPMatrix)o;
                    IloRange[] ranges = matrix.getRanges();
                    for (IloRange r: ranges)
                        addRange(rngs, oldRngNames, r);
                } else if (o instanceof IloObjective) {
// ignore
                } else if (o instanceof IloRange) {
                    addRange(rngs, oldRngNames, (IloRange) o);
                } else if (o instanceof IloNumVar) {
// ignore
                } else if (o instanceof IloConversion) {
                    // ignore
                } else
                    throw new IloException("Cannot handle " + o);
            }

            long t2 = new Date().getTime();
            logger.info("Naming stategy took " + (t2-t1)/1000 + " seconds.");
            // Now perform the solve
            result = externalSolve(vars.keySet(), rngs.keySet(), relaxer, conflicts);

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2val.entrySet()) {
                result.var2val.put(vars.get(e.getKey()), e.getValue());
            }
            result.name2val.clear();
            for (final Map.Entry<String, Double> e : result.name2ReducedCost.entrySet()) {
                result.var2ReducedCost.put(vars.get(e.getKey()), e.getValue());
            }
            result.name2ReducedCost.clear();

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2dual.entrySet()) {
                result.rng2dual.put(rngs.get(e.getKey()), e.getValue());
            }
            for (IloRange r: oldRngNames.keySet())
                    result.rng2dual.putIfAbsent(r, 0.0);
            result.name2dual.clear();

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2slack.entrySet()) {
                result.rng2slack.put(rngs.get(e.getKey()), e.getValue());
            }
            for (IloRange r: oldRngNames.keySet())
                result.rng2slack.putIfAbsent(r, 0.0);
            result.name2slack.clear();

            return result.feasible;
        } finally {
            // Restore original names if necessary.
            for (Map.Entry<IloNumVar, String> e : oldVarNames.entrySet())
                e.getKey().setName(e.getValue());

            // Restore original names if necessary.
            for (Map.Entry<IloRange, String> e : oldRngNames.entrySet())
                e.getKey().setName(e.getValue());
        }
    }

    /**
     * Perform an external solve.
     * The function must not return <code>null</code>.
     * All fields but {@link Solution#var2val} must be setup in the returned {@link Solution} instance.
     * Field {@link Solution#var2val} will be setup in {@link #solve()} from {@link Solution#name2val}. The latter
     * will also be cleared in {@link #solve()}.
     *
     * @param variables The names of variables known to the solver.
     * @return Solution information for the solve.
     * @throws IloException if anything goes wrong.
     */
    protected abstract Solution externalSolve(Set<String> variables, Set<String> ranges, Relaxations relax, Conflicts conflicts) throws IloException;

    // Below we overwrite a bunch of IloCplex functions that query solutions.
    // Add your own overwrites if you need more.

    @Override
    public double getObjValue() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        return result.objective;
    }



    @Override
    public double getValue(IloNumVar v) throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        final Double d = result.var2val.get(v);
        if (d == null)
            throw new IloException("Impossible to query variable value: Unkown variable "+ v + " in the solution.");
        return d;
    }

    @Override
    public double[] getValues(IloNumVar[] v) throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        final double[] ret = new double[v.length];
        for (int i = 0; i < v.length; ++i) {
            ret[i] = getValue(v[i]);
        }
        return ret;
    }

    @Override
    public double[] getValues(IloNumVar[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
        int size = var3-var2;
        if (size < 0) throw new IloException("Cannot get reduced cost: " + var2 + " " + var3);
        if (size == 0) return new double[0];
        double[] ret = new double[size];
        for (int i=var2; i< var3; i++)
            ret[i-var2] = getValue(var1[i]);
        return ret;
    }

        @Override
    public double getDual(IloRange r) throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        final Double d = result.rng2dual.get(r);
        if (d == null) throw new IloException("Impossible to query dual for range: Unknown range "+ r +" in the solution");
        return d;
    }

    @Override
    public double[] getDuals(IloRange[] r) throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        final double[] ret = new double[r.length];
        for (int i = 0; i < r.length; ++i) {
            ret[i] = getDual(r[i]);
        }
        return ret;
    }

    @Override
    public Status getStatus() throws IloException {
        if (result == null)
            return Status.Unknown;
        return makeStatus(getCplexStatus().getValue(), result.pfeas, result.dfeas);
    }

    @Override
    public CplexStatus getCplexStatus() throws IloException {
        if (result == null)
            return CplexStatus.Unknown;
        return CplexStatus.getStatus(result.status);
    }


    @Override
    public double getReducedCost(IloNumVar v) throws IloCplex.UnknownObjectException, IloException {
        if (result == null)
            throw new IloException("No solution available");
        final Double d = result.var2ReducedCost.get(v);
        if (d== null) throw new IloException("Impossible to get the reduced cost: Unkown variable "+v+ " in the solution");
        return d;
    }
    @Override
    public double[] getReducedCosts(IloNumVar[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
        int size = var3-var2;
        if (size < 0) throw new IloException("Cannot get reduced cost: " + var2 + " " + var3);
        if (size == 0) return new double[0];
        double[] ret = new double[size];
        for (int i=var2; i< var3; i++)
            ret[i-var2] = getReducedCost(var1[i]);
        return ret;
    }

    @Override
    public double[] getDuals(IloRange[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
        int size = var3-var2;
        if (size <0) throw new IloException("Cannot get Duals: "+var2 + " "+ var3);
        if (size == 0) return new double[0];
        IloRange[] values = new IloRange[size];
        for (int i= var2; i< var3; i++)
            values[i-var2] = var1[i];
        return getDuals(values);
    }

    @Override
    public double getSlack(IloRange r) throws IloCplex.UnknownObjectException, IloException {
        if (result == null)
            throw new IloException("No solution available");
        final Double d = result.rng2slack.get(r);
        if (d== null) throw new IloException("Impossible to query the slack for range "+r+ ": Unkown range in the solution");
        return d.doubleValue();
    }

    @Override
    public double[] getSlacks(IloRange[] r) throws IloCplex.UnknownObjectException, IloException {
        if (result == null)
            throw new IloException("No solution available");
        final double[] ret = new double[r.length];
        for (int i = 0; i < r.length; ++i) {
            ret[i] = getSlack(r[i]);
        }
        return ret;
    }
    @Override
    public double[] getSlacks(IloRange[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
        int size = var3-var2;
        if (size <0) throw new IloException("Cannot get Duals: "+var2 + " "+ var3);
        if (size == 0) return new double[0];
        IloRange[] values = new IloRange[size];
        for (int i= var2; i< var3; i++)
            values[i-var2] = var1[i];
        return getSlacks(values);
    }

    @Override
    public boolean isPrimalFeasible() throws IloException {
        // "real cplex does not always return the real value: sometimes false even if true. So instead of raising an error, raise a warning.
        logger.warn("isPrimalFeasible not implemented: always return false");
        return false;
    }
    @Override
    public boolean isDualFeasible() throws IloException {
        // "real cplex does not always return the real value: sometimes false even if true. So instead of raising an error, raise a warning.
        logger.warn("isDualFeasible not implemented: always return false");
        return false;
    }

    private double computeQuadExprSum(IloLQNumExpr quad) throws IloException {
        IloQuadNumExprIterator it = quad.quadIterator();
        double res = 0.0;
        while (it.hasNext()){
            res += getValue(it.getNumVar1())*getValue(it.getNumVar2())*it.getValue();
        }
        res += computeLinearExprSum(quad);
        return res;
    }
    private double computeQuadExprSum(IloLQIntExpr quad) throws IloException {
        IloQuadIntExprIterator it = quad.quadIterator();
        double res = 0.0;
        while (it.hasNext()){
            res += getValue(it.getIntVar1())*getValue(it.getIntVar2())*it.getValue();
        }
        res += computeLinearExprSum(quad);
        return res;
    }
    private double computeLinearExprSum(IloLinearNumExpr linear) throws IloException {
        IloLinearNumExprIterator it = linear.linearIterator();
        double res = 0.0;
        while (it.hasNext()){
            double val = getValue(it.nextNumVar())*it.getValue();
            res += val;
        }
        return res;
    }
    private double computeLinearExprSum(IloLinearIntExpr linear) throws IloException {
        IloLinearIntExprIterator it = linear.linearIterator();
        double res = 0.0;
        while (it.hasNext()){
            double val = it.getValue() * getValue(it.nextIntVar());
            res += val;
        }
        return res;
    }
    @Override
    public double getValue(IloNumExpr var) throws IloException {
        if (var instanceof IloLQNumExpr){
            IloLQNumExpr q = (IloLQNumExpr)var;
            return computeQuadExprSum(q)+q.getConstant();
        }
        if (var instanceof IloLQIntExpr){
            IloLQIntExpr q = (IloLQIntExpr)var;
            return computeQuadExprSum(q)+q.getConstant();
        }
        logger.error(notSupportedError+" getValue: non quad expr, non linear expr");
        throw new RuntimeException(notSupportedError);
    }


    @Override
    public boolean feasOpt(IloConstraint[] var1, double[] var2) throws IloException {
        return _feasOpt(var1, var2, null, null, null, null,null,null);
    }

    @Override
    public boolean feasOpt(IloRange[] var1, double[] var2, double[] var3, IloNumVar[] var4, double[] var5, double[] var6) throws IloException {
        logger.error(notSupportedError + " feasopt");
        throw new IloException(notSupportedError);
        //return _feasOpt(null, null, var1, var2, var3, var4,var5,var6);
    }

    @Override
    public boolean feasOpt(IloNumVar[] var1, double[] var2, double[] var3) throws IloException {
        logger.error(notSupportedError + " feasopt");
        throw new IloException(notSupportedError);
        //return _feasOpt(null, null, null, null, null, var1,var2,var3);
    }

    @Override
    public boolean feasOpt(IloRange[] var1, double[] var2, double[] var3) throws IloException {
        logger.error(notSupportedError + " feasopt");
        throw new IloException(notSupportedError);
        //return _feasOpt(null, null, var1, var2, var3, null,null,null);
    }

    @Override
    public IloCplex.ConflictStatus[] getConflict(IloConstraint[] var1) throws IloException {
        logger.error(notSupportedError + " getConflict");
        throw new IloException(notSupportedError);
    }
    @Override
    public IloCplex.ConflictStatus getConflict(IloConstraint var1) throws IloException {
        IloConstraint[] var2 = new IloConstraint[]{var1};
        return getConflict(var2)[0];
    }
    @Override
    public IloCplex.ConflictStatus[] getConflict(IloConstraint[] var1, int var2, int var3) throws IloException {
        int size = var3-var2;
        if (size <0) throw new IloException("Problem with conflict");
        if (size == 0) throw new IloException("Problem with conflict");
        IloConstraint[] cts = new IloConstraint[size];
        for (int i= var2; i< var3; i++) {
            cts[i - var2] = var1[i];
        }
        return getConflict(cts);
    }

    @Override
    public boolean refineConflict(IloConstraint[] var1, double[] var2) throws IloException {
        logger.error(notSupportedError + " refineConflict");
        throw new IloException(notSupportedError);
        /*if (var2.length != var1.length) throw new IloException("Problem with conflict");
        Conflicts refiner = new Conflicts();
        for (int i = 0; i< var1.length; i++)
            refiner.add(var1[i], var2[i]);

        return process(null, refiner);*/
    }

    @Override
    public boolean refineConflict(IloConstraint[] var1, double[] var2, int var3, int var4) throws IloException {
        if (var2.length != var1.length) throw new IloException("Problem with conflict");
        int size = var4-var3;
        if (size <0) throw new IloException("Problem with conflict");
        if (size == 0) throw new IloException("Problem with conflict");
        IloConstraint[] cts = new IloConstraint[size];
        double[] prefs = new double[size];
        for (int i= var3; i< var4; i++) {
            cts[i - var3] = var1[i];
            prefs[i - var3] = var2[i];
        }
        return refineConflict(cts, prefs);
    }
}
