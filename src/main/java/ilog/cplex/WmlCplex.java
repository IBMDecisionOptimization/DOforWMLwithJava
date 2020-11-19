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
import java.util.Date;
import java.util.Set;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ml.ilog.Connector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Job;
import ilog.concert.IloException;

public class WmlCplex extends ExternalCplex {
    private static final Logger logger = LogManager.getLogger();

    private final com.ibm.ml.ilog.v4.Connector wmlConnector;
    private final String wmlName;
    private final String cplexExportFormat;
    private final Integer timeLimit;
    private Job job = null;


    public WmlCplex(Credentials credentials, Connector.Runtime runtime, Connector.TShirtSize size, int numNodes) throws IloException {
        super();
        logger.info("Starting CPLEX with "+runtime+"."+size+"."+numNodes);
        wmlConnector = (com.ibm.ml.ilog.v4.Connector) Connector.getConnector(credentials, runtime, size, numNodes, "XML");
        wmlName = Connector.getCplexPrefix()+runtime+"."+size+"."+numNodes;
        cplexExportFormat = wmlConnector.config.getString("wmlconnector.v4.cplex_format");
        timeLimit = wmlConnector.config.getInt("wmlconnector.v4.time_limit");
        logger.info("Default time limit is "+ timeLimit + " minutes.");
    }

    @Override
    public void end(){
        logger.info("Cleaning the CPLEX job");
        if (job != null){
            logger.info("Cleaning remaining job "+job.getId());
            try {
                wmlConnector.deleteJob(job.getId());
            } catch (IloException e) {
                logger.warn("Ignoring error "+e.getMessage());
            }
        }
        wmlConnector.end();
        super.end();
    }

    @Override
    protected Solution externalSolve(Set<String> knownVariables, Set<String> knownConstraints, Relaxations relaxer, Conflicts conflicts) throws IloException {
        if (relaxer != null && conflicts != null)
            throw new IloException("Cannot run CPLEX with both relaxer and conflicts.");
        wmlConnector.initToken();
        if (getParam(Param.TimeLimit) == getDefault(Param.TimeLimit)){
            logger.info("Setting the time limit to default WML: "+timeLimit*60 + " seconds");
            setParam(Param.TimeLimit, timeLimit*60);
        }
        else{
            logger.info("Time limit has been set by user to "+getParam(Param.TimeLimit));
        }
        try {
            // Create temporary files for model input and solution output.
            final File model = File.createTempFile("cpx", cplexExportFormat);

            final File solution = new File(model.getAbsolutePath() + ".sol");
            solution.deleteOnExit();

            logger.info("Starting export");
            long t1 = new Date().getTime();
            exportModel(model.getAbsolutePath());
            long t2 = new Date().getTime();
            logger.info("Exported "+ cplexExportFormat + " file in " + (t2-t1)/1000 + " seconds");

            final File parameters = File.createTempFile("cpx", ".prm");
            writeParam(parameters.getAbsolutePath());

            boolean hasAnnotation = (getNumDoubleAnnotations() + getNumLongAnnotations()) != 0;

            // Filters
            File filters = null;
            File mst = null;
            File annotations = null;
            if (isMIP()) {
                filters = File.createTempFile("cpx", ".flt");
                writeFilters(filters.getAbsolutePath());

                // .MST
                try {
                    getMIPStart(0); //must be called to ensure that writeMIPStarts will not fail.
                    mst = File.createTempFile("cpx", ".mst");
                    writeMIPStarts(mst.getAbsolutePath());
                }
                catch (IloException e){
                    // IGNORE error.
                }
            }

            logger.info("Exported "+ cplexExportFormat + " file to " + model.getAbsolutePath());
            logger.info("Exported .prm file to " + parameters.getAbsolutePath());
            if (isMIP()){
                if (filters != null)
                    logger.info("Exported .flt file to " + filters.getAbsolutePath());
                if (mst != null)
                    logger.info("Exported .mst file to " + mst.getAbsolutePath());
            }
            if (hasAnnotation){
                annotations = File.createTempFile("cpx", ".ann");
                logger.info("Exported "+ (getNumDoubleAnnotations() + getNumLongAnnotations()) + " annotations to "+ annotations.getAbsolutePath());
                writeAnnotations(annotations.getAbsolutePath());
            }
            else
                logger.info("No annotation to export.");

            try {
                String deploymentId = wmlConnector.getOrMakeDeployment(wmlName, true);

                JSONArray input_data = new JSONArray();
                // Its parameters
                input_data.add(wmlConnector.createDataFromFile(wmlName +".prm", parameters.getAbsolutePath()));
                // Previous solution is any
                if (isMIP() && result != null && result.hasSolution()) // TODO only for MIP ?
                    input_data.add(wmlConnector.createDataFromString(wmlName +".sol", result.getSolution()));

                // Filters
                if (isMIP()) {
                    if (mst != null) input_data.add(wmlConnector.createDataFromFile(wmlName + ".mst", mst.getAbsolutePath()));
                    if (filters != null) input_data.add(wmlConnector.createDataFromFile(wmlName + ".flt", filters.getAbsolutePath()));
                }
                if (hasAnnotation)
                    input_data.add(wmlConnector.createDataFromFile(wmlName + ".ann", annotations.getAbsolutePath()));

                if (relaxer != null){
                    logger.info("Adding feasOpt support for "+relaxer.getSize() + " elements.");
                    if (relaxer.getSize() == 0)
                        logger.warn("Ignoring feasOpt as empty input.");
                    else
                        input_data.add(wmlConnector.createDataFromBytes(wmlName + "-relaxations.feasibility", relaxer.makeFile()));
                }
                if (conflicts != null){
                    logger.info("Adding conflict support for "+conflicts.getSize() + " elements.");
                    if (conflicts.getSize() ==0)
                        logger.warn("Ignoring conflicts as empty input.");
                    else
                        input_data.add(wmlConnector.createDataFromBytes(wmlName + "-conflicts.feasibility", conflicts.makeFile()));
                }

                long t3 = new Date().getTime();
                byte[] payload = wmlConnector.buildPayload(deploymentId, wmlName + cplexExportFormat, model.getAbsolutePath(), input_data, null);
                long t4 = new Date().getTime();
                logger.info("Building the payload took " + (t4 - t3) / 1000 + " seconds");

                job = wmlConnector.createAndRunEngineJob(deploymentId, payload);
                if (job.hasSolveState()) {
                    logger.info("SolveStatus = " + job.getSolveStatus());
                } else {
                    throw new IloException(job.getJobStatus().toString());
                }
                if (job.getSolveStatus().equals("infeasible_solution")) {
                    return new Solution(IloCplex.CplexStatus.Infeasible_Status);
                }
                String sol = job.getSolution();
                if (sol != null) {
                    // We have a feasible solution. Parse the solution file
                    FileWriter myWriter = new FileWriter(solution.getAbsolutePath());
                    myWriter.write(sol);
                    myWriter.close();
                    return new Solution(solution, knownVariables, knownConstraints);
                } else
                    return new Solution(CplexStatus.Unknown_Status);
            } finally {
                model.delete();
                parameters.delete();
                if (filters != null) filters.delete();
                solution.delete();
                if (job != null){
                    wmlConnector.deleteJob(job.getId());
                    job = null;
                }
                if (annotations != null) annotations.delete();
                wmlConnector.close();
            }
        }
        catch (java.lang.Exception e) { throw new IloException(e.getMessage()); }
    }
}
