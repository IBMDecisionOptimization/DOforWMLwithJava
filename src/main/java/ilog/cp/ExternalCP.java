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
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ilog.concert.IloAddable;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloIntVarArray;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloNumVar;
import ilog.concert.cppimpl.IloAlgorithm.Status;
import ilog.concert.cppimpl.IloIntervalSequenceVar;
import ilog.concert.cppimpl.IloIntervalSequenceVarArray;
import ilog.concert.cppimpl.IloIntervalVarArray;
import ilog.concert.cppimpl.IloStateFunction;
import ilog.concert.cppimpl.IloStateFunctionArray;

public abstract class ExternalCP extends NotSupportedCP {
    private static final Logger logger = LogManager.getLogger();

    private Status status = Status.Error;

    public ExternalCP() {
        super();
    }

    @Override
    public void end() {
        super.end();
    }


    /**
     *
     */
    protected static class Segment {
        final int start, end, value;

        public Segment(JSONObject segment) {
            if (segment.containsKey("start")) {
                Object val = segment.get("start");
                if (val.toString().equals("intervalmin")) {
                    start = IloCP.IntervalMin;
                } else {
                    start = ((Number) val).intValue();
                }
            } else {
                start = IloCP.IntervalMin;
            }

            if (segment.containsKey("end")) {
                Object val = segment.get("end");
                if (val.toString().equals("intervalmax")) {
                    end = IloCP.IntervalMax;
                } else {
                    end = ((Number) val).intValue();
                }
            } else {
                end = IloCP.IntervalMax;
            }

            if (segment.containsKey("value")) {
                Object val = segment.get("value");
                value = ((Number) val).intValue();
            } else {
                value = IloCP.NoState;
            }
        }
    }

    /**
     *
     */
    private static class VariablesMaps {
        final HashMap<String, IloNumVar> intVars;
        final HashMap<String, IloIntervalVar> intervalVars;
        final HashMap<String, IloIntervalSequenceVar> intervalSequenceVars;
        final HashMap<String, IloStateFunction> stateFunctions;
        final HashMap<String, String> constraints;

        public VariablesMaps(HashMap<String, IloNumVar> intVars, HashMap<String, IloIntervalVar> intervalVars,
                             HashMap<String, IloIntervalSequenceVar> intervalSequenceVars,
                             HashMap<String, IloStateFunction> stateFunctions,
                             HashMap<String, String> constraints) {
            this.intVars = intVars;
            this.intervalVars = intervalVars;
            this.intervalSequenceVars = intervalSequenceVars;
            this.stateFunctions = stateFunctions;
            this.constraints = constraints;
        }
    }


    /**
     *
     */
    protected static class Solution {

        /**
         * Map variable names to values.
         */
        public Map<String, Double> name2val = new HashMap<>();
        public Map<String, Double> name2start = new HashMap<>();
        public Map<String, Double> name2size = new HashMap<>();
        public Map<String, Double> name2end = new HashMap<>();
        public Map<String, List<String>> name2nameList = new HashMap<>();
        public Map<String, List<Segment>> name2segmentList = new HashMap<>();
        /**
         * Map variable objects to values.
         */
        public Map<IloNumVar, Double> var2val = new HashMap<>();
        public Map<IloIntervalVar, Double> var2start = new HashMap<>();
        public Map<IloIntervalVar, Double> var2size = new HashMap<>();
        public Map<IloIntervalVar, Double> var2end = new HashMap<>();
        public Map<IloIntervalSequenceVar, List<IloIntervalVar>> var2intervalList = new HashMap<>();
        public Map<IloStateFunction, List<Segment>> var2segmentList = new HashMap<>();

        public List<Double> objectiveValues = new ArrayList<>();
        public List<Double> boundValues = new ArrayList<>();
        public List<Double> gapValues = new ArrayList<>();
        public Map<String, Double> kpi2val = new HashMap<>();

        public Map<String, String> conflictingConstraints = new HashMap<>();
        public Map<String, String> conflictingIntervalVars = new HashMap<>();
        public List<String> conflictingConstraintList = new ArrayList<>();
        public List<String> conflictingIntervalVarsList = new ArrayList<>();

        private Status solutionStatus = null;
        private boolean hasConflicts = false;

        public Solution() {
        }

        public Solution(JSONObject solutionJson) {
            this();
            parse(solutionJson);
        }

        public void reset() {
            name2val = new HashMap<>();
            var2val = new HashMap<>();

            name2start = new HashMap<>();
            var2start = new HashMap<>();

            name2size = new HashMap<>();
            var2size = new HashMap<>();

            name2end = new HashMap<>();
            var2end = new HashMap<>();

            objectiveValues = new ArrayList<>();
            boundValues = new ArrayList<>();
            gapValues = new ArrayList<>();

            kpi2val = new HashMap<>();
            /*
             * objective = Double.NaN; status = -1; pfeas = false; dfeas = false;
             */
            conflictingConstraints = new HashMap<>();
            conflictingIntervalVars = new HashMap<>();
            conflictingConstraintList = new ArrayList<>();
            conflictingIntervalVarsList = new ArrayList<>();

            hasConflicts = false;
        }

        private Status getStatus(String solveStatus) {
            switch (solveStatus) {
                case "Feasible":
                    return Status.Feasible;
                case "Optimal":
                    return Status.Optimal;
                case "Infeasible":
                    return Status.Infeasible;
                case "InfeasibleOrUnbounded":
                    return Status.InfeasibleOrUnbounded;
                case "Unbounded":
                    return Status.Unbounded;
                case "Unknown":
                    return Status.Unknown;
                default:
                    return Status.Error;
            }
        }

        private Status getStatus(JSONObject solutionJson) {
            if (solutionJson.containsKey("solutionStatus")) {
                return getStatus(
                        (String) ((JSONObject) solutionJson.get("solutionStatus")).get("solveStatus"));
            }
            return null;
        }

        protected Status getSolutionStatus() {
            return solutionStatus;
        }

        private double parseDoubleFromString(String valueAsStr) {
            if (valueAsStr.equalsIgnoreCase("infinity"))
                return Double.POSITIVE_INFINITY;
            return Double.parseDouble(valueAsStr);
        }

        private void parse(JSONObject solutionJson) {
            reset();

            solutionStatus = getStatus(solutionJson);
            if (solutionJson.containsKey("intVars")) {
                JSONObject intVars = (JSONObject) solutionJson.get("intVars");
                for (Object o : intVars.keySet()) {
                    String name = (String) o;
                    Number value = ((Number) intVars.get(name));
                    name2val.put(name, value.doubleValue());
                }
            }

            if (solutionJson.containsKey("intervalVars")) {
                JSONObject intervalVars = (JSONObject) solutionJson.get("intervalVars");
                for (Object o : intervalVars.keySet()) {
                    String name = (String) o;
                    if (((JSONObject) intervalVars.get(name)).containsKey("start")) {
                        double start = ((Number) ((JSONObject) intervalVars.get(name)).get("start")).doubleValue();
                        name2start.put(name, start);
                        double size = ((Number) ((JSONObject) intervalVars.get(name)).get("size")).doubleValue();
                        name2size.put(name, size);
                        double end = ((Number) ((JSONObject) intervalVars.get(name)).get("end")).doubleValue();
                        name2end.put(name, end);
                    }
                }
            }

            if (solutionJson.containsKey("sequenceVars")) {
                JSONObject intervalSequenceVars = (JSONObject) solutionJson.get("sequenceVars");
                for (Object o : intervalSequenceVars.keySet()) {
                    String name = (String) o;
                    List<String> interval_seq_ids = new ArrayList<>();
                    JSONArray intervalSequence = (JSONArray) intervalSequenceVars.get(name);
                    for (Object value : intervalSequence) {
                        String interval_id = value.toString();
                        interval_seq_ids.add(interval_id);
                    }
                    name2nameList.put(name, interval_seq_ids);
                }
            }

            if (solutionJson.containsKey("stateFunctions")) {
                JSONObject stateFunctions = (JSONObject) solutionJson.get("stateFunctions");
                for (Object name : stateFunctions.keySet()) {
                    List<Segment> state_function_segments = new ArrayList<>();
                    JSONArray stateFunction = (JSONArray) stateFunctions.get(name);
                    for (Object o : stateFunction) {
                        JSONObject segment = (JSONObject) o;
                        state_function_segments.add(new Segment(segment));
                    }
                    name2segmentList.put((String) name, state_function_segments);
                }
            }

            if (solutionJson.containsKey("KPIs")) {
                JSONObject kpis = (JSONObject) solutionJson.get("KPIs");
                for (Object name : kpis.keySet()) {
                    Double value = ((Double) kpis.get(name));
                    kpi2val.put((String) name, value);
                }
            }

            if (solutionJson.containsKey("objectives")) {
                JSONArray objectives = (JSONArray) solutionJson.get("objectives");
                for (Object objValue : objectives) {
                    objectiveValues.add(parseDoubleFromString(objValue.toString()));
                }
            }

            if (solutionJson.containsKey("bounds")) {
                JSONArray bounds = (JSONArray) solutionJson.get("bounds");
                for (Object boundValue : bounds) {
                    boundValues.add(parseDoubleFromString(boundValue.toString()));
                }
            }

            if (solutionJson.containsKey("gaps")) {
                JSONArray gaps = (JSONArray) solutionJson.get("gaps");
                for (Object gapValue : gaps) {
                    gapValues.add(parseDoubleFromString(gapValue.toString()));
                }
            }

            if (solutionJson.containsKey("conflict")) {
                JSONObject conflict = (JSONObject) solutionJson.get("conflict");
                if (conflict.containsKey("constraints")) {
                    hasConflicts = true;
                    JSONObject constraints = (JSONObject) conflict.get("constraints");
                    for (Object constraintId : constraints.keySet()) {
                        String conflictStatus = ((String) constraints.get(constraintId));
                        conflictingConstraints.put((String) constraintId, conflictStatus);
                    }
                }
                if (conflict.containsKey("intervalVars")) {
                    hasConflicts = true;
                    JSONObject intervalVars = (JSONObject) conflict.get("intervalVars");
                    for (Object intervalVarId : intervalVars.keySet()) {
                        String conflictStatus = ((String) intervalVars.get(intervalVarId));
                        conflictingIntervalVars.put((String) intervalVarId, conflictStatus);
                    }
                }
            }
        }
    }

    private Solution result = null;

    /**
     * Keep track of original name for variables that are renamed before exporting the model to a ".cpo" file.
     */
    private final Map<IloAddable, String> renamedVarToOriginName = new HashMap<>();

    /**
     * Set a unique name in case:
     * - variable has no name (that is: name == null)
     * - variable name is a duplicate (same name is used by multiple variables of same type)
     * After parsing the solution, original names are restored by invoking the restoreRenamedVarsToOriginName() method.
     *
     * @param var           The variable to be assigned a unique name if necessary
     * @param prefix        The prefix to be used for creating new names (when variable name is null)
     * @param existingNames Collection of existing names to detect duplicates
     */
    private void setUniqueName(IloAddable var, String prefix, Set<String> existingNames) {
        String originName = var.getName();
        String name = (originName == null ? prefix : originName);
        String actualName = name;
        int counter = 0;
        while (existingNames.contains(actualName)) {
            actualName = name + "_" + (++counter);
        }
        if (!actualName.equals(originName)) {
            var.setName(actualName);
            renamedVarToOriginName.put(var, originName);
        }
    }

    /**
     * Restore original name for model variables that have been renamed before exporting the model to a ".cpo" file.
     */
    private void restoreRenamedVarsToOriginName() {
        for (Entry<IloAddable, String> e : renamedVarToOriginName.entrySet()) {
            e.getKey().setName(e.getValue());
        }
    }

    /**
     * If multiple model objects have same name,
     */
    private void checkNoDuplicateNameInModel(String intervalVarName) throws IloException {
        if (renamedVarToOriginName.containsValue(intervalVarName)) {
            throw new IloException("Duplicate model object name " + intervalVarName);
        }
    }

    /**
     *
     */
    @SuppressWarnings("rawtypes")
	private VariablesMaps buildVariableMaps() {
        // Clear renamed variables map
        renamedVarToOriginName.clear();

        final HashMap<String, IloNumVar> intVars = new HashMap<>();
        IloIntVarArray allIntVars = getCPImpl().getAllIloIntVars();
        for (int i = 0; i < allIntVars.getSize(); i++) {
            IloIntVar v = allIntVars.getIntVar(i);
            setUniqueName(v, "IntVar_" + i, intVars.keySet());
            intVars.put(v.getName(), v);
        }

        final HashMap<String, IloIntervalVar> intervalVars = new HashMap<>();
        try {
            IloIntervalVarArray allIntervalVars = getCPImpl().getAllIloIntervalVars();
            for (int i = 0; i < allIntervalVars.getSize(); i++) {
                IloIntervalVar intervalVar = allIntervalVars.get_IloIntervalVar(i);
                setUniqueName(intervalVar, "IntervalVar_" + i, intervalVars.keySet());
                intervalVars.put(intervalVar.getName(), intervalVar);
            }
        } catch (Exception e) {
            // Ignore
        }

        final HashMap<String, IloIntervalSequenceVar> intervalSequenceVars = new HashMap<>();
        try {
            IloIntervalSequenceVarArray allIloIntervalSequenceVars = getCPImpl().getAllIloIntervalSequenceVars();
            for (int i = 0; i < allIloIntervalSequenceVars.getSize(); i++) {
                IloIntervalSequenceVar intervalSequence = allIloIntervalSequenceVars.get_IloIntervalSequenceVar(i);
                setUniqueName(intervalSequence, "IntervalSequenceVar_" + i, intervalSequenceVars.keySet());
                intervalSequenceVars.put(intervalSequence.getName(), intervalSequence);
            }
        } catch (Exception e) {
            // Ignore
        }

        final HashMap<String, IloStateFunction> stateFunctions = new HashMap<>();
        try {
            IloStateFunctionArray allIloStateFunctions = getCPImpl().getAllIloStateFunctions();
            for (int i = 0; i < allIloStateFunctions.getSize(); i++) {
                IloStateFunction stateFunction = allIloStateFunctions.get_IloStateFunction(i);
                setUniqueName(stateFunction, "StateFunction_" + i, stateFunctions.keySet());
                stateFunctions.put(stateFunction.getName(), stateFunction);
            }

        } catch (Exception e) {
            // Ignore
        }

        Iterator iterator = this.iterator();
        final HashMap<String, String> constraints = new HashMap<>();
        int constraint_counter = 0;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o instanceof ilog.concert.cppimpl.IloConstraint) {
                ilog.concert.cppimpl.IloConstraint constraint = (ilog.concert.cppimpl.IloConstraint) o;
                // Need to retrieve the String representation of the constraint before setting a default name (named constraints are displayed with their name)
                String constraintAsStr = constraint.toString();
                setUniqueName(constraint, "Constraint_" + (++constraint_counter), constraints.keySet());
                constraints.put(constraint.getName(), constraintAsStr);
            }
        }
        return new VariablesMaps(intVars, intervalVars, intervalSequenceVars, stateFunctions, constraints);
    }


    protected void resetStatus() {
        this.status = Status.Error;
    }


    @Override
    public boolean solve() throws IloException {
        VariablesMaps variableMaps = buildVariableMaps();

        // Now perform the solve
        String solveStatus = externalSolve();
        logger.info("SolveStatus = " + solveStatus);

        result = null;
        if (solveStatus.equals("infeasible_solution")) {
            status = Status.Infeasible;

        } else {
            // We have a feasible solution. Parse the solution file
            result = getSolution();
            status = result.getSolutionStatus();

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2val.entrySet()) {
                result.var2val.put(variableMaps.intVars.get(e.getKey()), e.getValue());
            }
            // Keep name2val map as it is used for getters using interval name
            //  result.name2val.clear();

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2start.entrySet()) {
                result.var2start.put(variableMaps.intervalVars.get(e.getKey()), e.getValue());
            }
            // Keep name2start map as it is used for getters using interval name
            //  result.name2start.clear();

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2size.entrySet()) {
                result.var2size.put(variableMaps.intervalVars.get(e.getKey()), e.getValue());
            }
            // Keep name2size map as it is used for getters using interval name
            //  result.name2size.clear();

            // Transfer non-zeros indexed by name to non-zeros indexed by object.
            for (final Map.Entry<String, Double> e : result.name2end.entrySet()) {
                result.var2end.put(variableMaps.intervalVars.get(e.getKey()), e.getValue());
            }
            // Keep name2end map as it is used for getters using interval name
            //  result.name2end.clear();

            //
            for (final Entry<String, List<String>> e : result.name2nameList.entrySet()) {
                List<IloIntervalVar> intervalList = new ArrayList<>();
                for (final String interval_id : e.getValue()) {
                    intervalList.add(variableMaps.intervalVars.get(interval_id));
                }
                result.var2intervalList.put(variableMaps.intervalSequenceVars.get(e.getKey()), intervalList);
            }

            //
            for (final Entry<String, List<Segment>> e : result.name2segmentList.entrySet()) {
                result.var2segmentList.put(variableMaps.stateFunctions.get(e.getKey()), e.getValue());
            }
        }

        // Restore original names of renamed variables
        restoreRenamedVarsToOriginName();

        return status == Status.Feasible || status == Status.Optimal;
    }


    @Override
    public boolean refineConflict() throws IloException {
        VariablesMaps variableMaps = buildVariableMaps();

        //
        String solveStatus = externalRefineConflict();
        logger.info("RefineConflict SolveStatus = " + solveStatus);

        result = getSolution();
        status = result.getSolutionStatus();

        if (result.hasConflicts) {

            // Transfer conflicting constraints indexed by name to conflict indexed by object
            for (Entry<String, String> conflictingConstraint : result.conflictingConstraints.entrySet()) {
                result.conflictingConstraintList.add(variableMaps.constraints.get(conflictingConstraint.getKey()));
            }
            for (Entry<String, String> conflictingIntervalVar : result.conflictingIntervalVars.entrySet()) {
                result.conflictingIntervalVarsList.add(variableMaps.intervalVars.get(conflictingIntervalVar.getKey()).toString());
            }

            return true;
        }
        return false;
    }


    @Override
    public void writeConflict() throws IloException {
        writeConflict(System.out);
    }


    @Override
    public void writeConflict(OutputStream os) throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        PrintWriter pw = new PrintWriter(os);
        if (result.hasConflicts) {
            pw.println("// ------ Conflict members: ---------------------------------------------------");
            for (String constraint : result.conflictingConstraintList) {
                pw.println(constraint);
            }
            for (String intervalVar : result.conflictingIntervalVarsList) {
                pw.println(intervalVar);
            }
        }
        pw.flush();
        pw.close();
    }

    /**
     * Perform an external solve.
     */
    protected abstract String externalSolve() throws IloException;

    protected abstract String externalRefineConflict() throws IloException;

    protected abstract Solution getSolution() throws IloException;

    // Below we overwrite a bunch of IloCP functions that query solutions.
    // Add your own overwrites if you need more.

    @Override
    public double getObjValue() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        return getObjValues()[0]; // Returns first objective value in list
    }

    @Override
    public double getObjValue(int i) throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        if (result.objectiveValues == null)
            throw new IloException("No objective defined");
        if (i >= result.objectiveValues.size())
            throw new IloException("Invalid index for objective value: " + i);
        return result.objectiveValues.get(i);
    }

    @Override
    public double[] getObjValues() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        if (result.objectiveValues == null)
            throw new IloException("No objective defined");
        double[] res = new double[result.objectiveValues.size()];
        for (int index = 0; index < res.length; index++) {
            res[index] = result.objectiveValues.get(index);
        }
        return res;
    }

    @Override
    public double getObjBound() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        return getObjBounds()[0]; // Returns first bound value in list
    }

    @Override
    public double getObjBound(int i) throws IloException {
        double[] boundValues = getObjBounds();
        if (i >= boundValues.length)
            throw new IloException("Invalid index for objective bound: " + i);
        return boundValues[i];
    }

    @Override
    public double[] getObjBounds() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        if (result.boundValues == null)
            throw new IloException("No objective bounds");
        double[] res = new double[result.boundValues.size()];
        for (int index = 0; index < res.length; index++) {
            res[index] = result.boundValues.get(index);
        }
        return res;
    }

    @Override
    public double getObjGap() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        return getObjGaps()[0]; // Returns first bound value in list
    }

    @Override
    public double getObjGap(int i) throws IloException {
        double[] gapValues = getObjBounds();
        if (i >= gapValues.length)
            throw new IloException("Invalid index for objective gap: " + i);
        return gapValues[i];
    }

    @Override
    public double[] getObjGaps() throws IloException {
        if (result == null)
            throw new IloException("No solution available");
        if (result.gapValues == null)
            throw new IloException("No objective gaps");
        double[] res = new double[result.gapValues.size()];
        for (int index = 0; index < res.length; index++) {
            res[index] = result.gapValues.get(index);
        }
        return res;
    }

    @Override
    public int getIntValue(IloIntVar v) {
        if (result == null)
            return super.getIntValue(v);
        if (result.var2val.containsKey(v))
            return result.var2val.get(v).intValue();
        else
            return super.getIntValue(v);
    }

    @Override
    public double getValue(IloNumVar v) {
        if (result == null)
            return super.getValue(v);
        if (result.var2val.containsKey(v))
            return result.var2val.get(v);
        else
            return super.getValue(v);
    }

    @Override
    public int getValue(String intVarName) throws IloException {
        checkNoDuplicateNameInModel(intVarName);
        if (result == null)
            return super.getValue(intVarName);
        if (result.name2val.containsKey(intVarName))
            return result.name2val.get(intVarName).intValue();
        else
            return super.getValue(intVarName);
    }

    @Override
    public boolean isPresent(String intervalVarName) throws IloException {
        checkNoDuplicateNameInModel(intervalVarName);
        if (result == null)
            return false;
        return result.name2start.containsKey(intervalVarName);
    }

    @Override
    public boolean isPresent(IloIntervalVar a) {
        if (result == null)
            return false;
        return result.var2start.containsKey(a);
    }

    @Override
    public boolean isAbsent(String intervalVarName) throws IloException {
        checkNoDuplicateNameInModel(intervalVarName);
        return !isPresent(intervalVarName);
    }

    @Override
    public boolean isAbsent(IloIntervalVar a) {
        return !isPresent(a);
    }

    @Override
    public int getEnd(IloIntervalVar a) {
        if (result == null)
            return super.getEnd(a);
        if (result.var2end.containsKey(a))
            return result.var2end.get(a).intValue();
        else
            return super.getEnd(a);
    }

    @Override
    public int getEnd(String intervalVarName) throws IloException {
        checkNoDuplicateNameInModel(intervalVarName);
        if (result == null)
            return super.getEnd(intervalVarName);
        if (result.name2end.containsKey(intervalVarName))
            return result.name2end.get(intervalVarName).intValue();
        else
            return super.getEnd(intervalVarName);
    }

    @Override
    public void getValues(IloIntVar[] vars, double[] vals) throws IloException {
        if (result == null)
            super.getValues(vars, vals);
        if ((vars == null) || (vals == null))
            throw new IloException("Input arrays should not be null");
        if (vars.length != vals.length)
            throw new IloException("Input arrays length mismatch");
        for (int i = 0; i < vars.length; i++)
            vals[i] = getValue(vars[i]);
    }

    @Override
    public void getValues(IloIntVarArray varArray, double[] numArray) throws IloException {
        if (result == null)
            super.getValues(varArray, numArray);
        if ((varArray == null) || (numArray == null))
            throw new IloException("Input arrays should not be null");
        if (varArray.getSize() != numArray.length)
            throw new IloException("Input arrays length mismatch");

        for (int i = 0; i < varArray.getSize(); i++) {
            IloIntVar v = varArray.getIntVar(i);
            numArray[i] = getValue(v);
        }
    }

    @Override
    public void getValues(IloNumVar[] varArray, double[] numArray) throws IloException {
        if (result == null)
            super.getValues(varArray, numArray);
        if ((varArray == null) || (numArray == null))
            throw new IloException("Input arrays should not be null");
        if (varArray.length != numArray.length)
            throw new IloException("Input arrays length mismatch");
        for (int i = 0; i < varArray.length; i++)
            numArray[i] = getValue(varArray[i]);
    }

    @Override
    public int getStart(IloIntervalVar a) {
        if (result == null)
            return super.getStart(a);
        if (result.var2start.containsKey(a))
            return result.var2start.get(a).intValue();
        else
            return super.getStart(a);
    }

    @Override
    public int getStart(String intervalVarName) throws IloException {
        checkNoDuplicateNameInModel(intervalVarName);
        if (result == null)
            return super.getStart(intervalVarName);
        if (result.name2end.containsKey(intervalVarName))
            return result.name2start.get(intervalVarName).intValue();
        else
            return super.getStart(intervalVarName);
    }

    @Override
    public int getStartMin(IloIntervalVar a) {
        if (result == null)
            return a.getStartMin();
        if (result.var2start.containsKey(a))
            return result.var2start.get(a).intValue();
        else
            return a.getStartMin();
    }

    @Override
    public int getStartMax(IloIntervalVar a) {
        if (result == null)
            return a.getStartMax();
        if (result.var2start.containsKey(a))
            return result.var2start.get(a).intValue();
        else
            return a.getStartMax();
    }

    @Override
    public boolean isFixed(IloIntervalVar a) {
        if (isAbsent(a))
            return true;
        return isPresent(a) && (getSizeMin(a) == getSizeMax(a)) && (getStartMin(a) == getStartMax(a))
                && (getEndMin(a) == getEndMax(a));
    }

    @Override
    public int getSize(IloIntervalVar a) {
        if (result == null)
            return super.getSize(a);
        return getSize(a.getName());
    }

    @Override
    public int getSize(String intervalVarName) {
        try {
            checkNoDuplicateNameInModel(intervalVarName);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        if (result == null)
            return super.getSize(intervalVarName);
        if (result.name2size.containsKey(intervalVarName))
            return result.name2size.get(intervalVarName).intValue();
        else
            return super.getSize(intervalVarName);
    }

    @Override
    public int getSizeMin(IloIntervalVar a) {
        if (result == null)
            return a.getSizeMin();
        if (result.var2size.containsKey(a))
            return result.var2size.get(a).intValue();
        else
            return a.getSizeMin();
    }

    @Override
    public int getSizeMax(IloIntervalVar a) {
        if (result == null)
            return a.getSizeMax();
        if (result.var2size.containsKey(a))
            return result.var2size.get(a).intValue();
        else
            return a.getSizeMax();
    }

    @Override
    public int getLength(IloIntervalVar a) {
        if (result == null)
            return super.getLength(a);
        return getEnd(a) - getStart(a);
    }

    @Override
    public int getLength(String intervalVarName) throws IloException {
        checkNoDuplicateNameInModel(intervalVarName);
        if (result == null)
            return super.getSize(intervalVarName);
        return getEnd(intervalVarName) - getStart(intervalVarName);
    }

    @Override
    public int getLengthMin(IloIntervalVar a) {
        if (result == null)
            return a.getLengthMin();
        if (result.var2start.containsKey(a)) {
            int startMax = this.getStartMax(a);
            int endMin = this.getEndMin(a);
            return endMin - startMax;
        } else
            return a.getLengthMin();
    }

    @Override
    public int getLengthMax(IloIntervalVar a) {
        if (result == null)
            return a.getLengthMax();
        if (result.var2start.containsKey(a)) {
            int startMin = this.getStartMin(a);
            int endMax = this.getEndMax(a);
            return endMax - startMin;
        } else
            return a.getLengthMax();
    }

    @Override
    public int getEndMin(IloIntervalVar a) {
        if (result == null)
            return a.getEndMin();
        if (result.var2end.containsKey(a))
            return result.var2end.get(a).intValue();
        else
            return a.getEndMin();
    }

    @Override
    public int getEndMax(IloIntervalVar a) {
        if (result == null)
            return a.getEndMax();
        if (result.var2end.containsKey(a))
            return result.var2end.get(a).intValue();
        else
            return a.getEndMax();
    }

    @Override
    public double getKPIValue(String name) throws IloException {
        if (result == null)
            return super.getKPIValue(name);
        if (result.kpi2val.containsKey(name))
            return result.kpi2val.get(name);
        else
            return super.getKPIValue(name);
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getStatusString() {
        return getStatus().toString();
    }

	@SuppressWarnings({"SuspiciousMethodCalls"})
	@Override
    public IloIntervalVar getFirst(ilog.concert.IloIntervalSequenceVar seq) throws IloException {
        if (result == null)
            return null;
        List<IloIntervalVar> list = result.var2intervalList.getOrDefault(seq, new LinkedList<>());
        if (list.size() > 0) {
            return list.get(0);
        } else
            return null;
    }

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public IloIntervalVar getLast(ilog.concert.IloIntervalSequenceVar seq) throws IloException {
        if (result == null)
            return null;
        List<IloIntervalVar> list = result.var2intervalList.getOrDefault(seq, new LinkedList<>());
        if (list.size() > 0) {
            return list.get(list.size() - 1);
        } else
            return null;
    }

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public IloIntervalVar getNext(ilog.concert.IloIntervalSequenceVar seq, IloIntervalVar a) throws IloException {
        if (result == null)
            return null;
        List<IloIntervalVar> list = result.var2intervalList.getOrDefault(seq, new LinkedList<>());
        int lastIndexOf = list.lastIndexOf(a);
        if ((lastIndexOf >= 0) && (lastIndexOf < list.size() - 1)) {
            return list.get(lastIndexOf + 1);
        } else
            return null;
    }

    @Override
    public IloSearchPhase searchPhase(IloIntVar[] vars, IloIntVarChooser varChooser, IloIntValueChooser valueChooser)
            throws IloException {
        if (!(varChooser instanceof ilog.cp.cppimpl.IloIntVarChooser)) {
            logger.error(notSupportedError + " searchPhase with custom IloIntVarChooser");
            throw new RuntimeException(notSupportedError);
        }
        if (!(valueChooser instanceof ilog.cp.cppimpl.IloIntValueChooser)) {
            logger.error(notSupportedError + " searchPhase with custom IloIntValueChooser");
            throw new RuntimeException(notSupportedError);
        }
        return super.searchPhase(vars, varChooser, valueChooser);
    }

    @Override
    public IloSearchPhase searchPhase(IloIntVarArray vars, IloIntVarChooser varChooser, IloIntValueChooser valueChooser)
            throws IloException {
        if (!(varChooser instanceof ilog.cp.cppimpl.IloIntVarChooser)) {
            logger.error(notSupportedError + " searchPhase with custom IloIntVarChooser");
            throw new RuntimeException(notSupportedError);
        }
        if (!(valueChooser instanceof ilog.cp.cppimpl.IloIntValueChooser)) {
            logger.error(notSupportedError + " searchPhase with custom IloIntValueChooser");
            throw new RuntimeException(notSupportedError);
        }
        return super.searchPhase(vars, varChooser, valueChooser);
    }

    @Override
    public IloSearchPhase searchPhase(IloIntVarChooser varChooser, IloIntValueChooser valueChooser)
            throws IloException {
        if (!(varChooser instanceof ilog.cp.cppimpl.IloIntVarChooser)) {
            logger.error(notSupportedError + " searchPhase with custom IloIntVarChooser");
            throw new RuntimeException(notSupportedError);
        }
        if (!(valueChooser instanceof ilog.cp.cppimpl.IloIntValueChooser)) {
            logger.error(notSupportedError + " searchPhase with custom IloIntValueChooser");
            throw new RuntimeException(notSupportedError);
        }
        return super.searchPhase(varChooser, valueChooser);
    }

    @Override
    public IloValueSelector selectLargest(double minNumber, IloIntValueEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntValueEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntValueEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectLargest(minNumber, e);
    }

    @Override
    public IloVarSelector selectLargest(double minNumber, IloIntVarEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntVarEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntVarEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectLargest(minNumber, e);
    }

    @Override
    public IloValueSelector selectLargest(IloIntValueEval e, double tol) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntValueEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntValueEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectLargest(e, tol);
    }

    @Override
    public IloValueSelector selectLargest(IloIntValueEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntValueEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntValueEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectLargest(e);
    }

    @Override
    public IloVarSelector selectLargest(IloIntVarEval e, double tol) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntVarEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntVarEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectLargest(e, tol);
    }

    @Override
    public IloVarSelector selectLargest(IloIntVarEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntVarEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntVarEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectLargest(e);
    }

    @Override
    public IloValueSelector selectSmallest(double minNumber, IloIntValueEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntValueEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntValueEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectSmallest(minNumber, e);
    }

    @Override
    public IloVarSelector selectSmallest(double minNumber, IloIntVarEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntVarEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntVarEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectSmallest(minNumber, e);
    }

    @Override
    public IloValueSelector selectSmallest(IloIntValueEval e, double tol) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntValueEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntValueEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectSmallest(e, tol);
    }

    @Override
    public IloValueSelector selectSmallest(IloIntValueEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntValueEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntValueEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectSmallest(e);
    }

    @Override
    public IloVarSelector selectSmallest(IloIntVarEval e, double tol) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntVarEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntVarEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectSmallest(e, tol);
    }

    @Override
    public IloVarSelector selectSmallest(IloIntVarEval e) throws IloException {
        if (!(e instanceof ilog.cp.cppimpl.IloIntVarEval)) {
            logger.error(notSupportedError + " selectLargest with custom IloIntVarEval");
            throw new RuntimeException(notSupportedError);
        }
        return super.selectSmallest(e);
    }

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public int getNumberOfSegments(ilog.concert.IloStateFunction f) throws IloException {
        if (result == null)
            return IloCP.NoState;
        List<Segment> list = result.var2segmentList.getOrDefault(f, new LinkedList<>());
        return list.size();
    }

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public int getSegmentStart(ilog.concert.IloStateFunction f, int i) throws IloException {
        if (result == null)
            return IloCP.NoState;
        List<Segment> list = result.var2segmentList.getOrDefault(f, new LinkedList<>());
        if ((i >= 0) && (i < list.size())) {
            return list.get(i).start;
        }
        return IloCP.NoState;
    }

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public int getSegmentEnd(ilog.concert.IloStateFunction f, int i) throws IloException {
        if (result == null)
            return IloCP.NoState;
        List<Segment> list = result.var2segmentList.getOrDefault(f, new LinkedList<>());
        if ((i >= 0) && (i < list.size())) {
            return list.get(i).end;
        }
        return IloCP.NoState;
    }

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public int getSegmentValue(ilog.concert.IloStateFunction f, int i) throws IloException {
        if (result == null)
            return IloCP.NoState;
        List<Segment> list = result.var2segmentList.getOrDefault(f, new LinkedList<>());
        if ((i >= 0) && (i < list.size())) {
            return list.get(i).value;
        }
        return IloCP.NoState;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
	@Override
    public int getValue(ilog.concert.IloStateFunction f, int t) throws IloException {
        if (result == null)
            return IloCP.NoState;
        for (Segment segment : result.var2segmentList.getOrDefault(f, new LinkedList<>())) {
            if ((t >= segment.start) && (t < segment.end)) {
                return segment.value;
            }
        }
        return IloCP.NoState;
    }

    @Override
    public boolean isFixed(ilog.concert.IloStateFunction f) throws IloException {
        return result != null;
    }
}
