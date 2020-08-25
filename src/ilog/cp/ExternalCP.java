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

import ilog.concert.*;
import ilog.concert.cppimpl.IloIntervalVarArray;
import ilog.cplex.ExternalCplex;
import ilog.cplex.IloCplex;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Base class for external solves.
 * @author daniel.junglas@de.ibm.com
 *
 */
public abstract class ExternalCP extends IloCP {
  private static final long serialVersionUID = 1;


  public ExternalCP() throws IloException {
    super();
  }


  protected static class Solution {

    boolean feasible = false;
    /** Map variable names to values. */
    public HashMap<String,Double> name2val = new HashMap<String,Double>();
    public HashMap<String,Double> name2start = new HashMap<String,Double>();
    public HashMap<String,Double> name2size = new HashMap<String,Double>();
    public HashMap<String,Double> name2end = new HashMap<String,Double>();
    /** Map variable objects to values. */
    public HashMap<IloNumVar, Double> var2val = new HashMap<IloNumVar, Double>();
    public HashMap<IloIntervalVar, Double> var2start = new HashMap<IloIntervalVar, Double>();
    public HashMap<IloIntervalVar, Double> var2size = new HashMap<IloIntervalVar, Double>();
    public HashMap<IloIntervalVar, Double> var2end = new HashMap<IloIntervalVar, Double>();

    public Solution() {
    }

    public Solution(JSONObject solutionJson, Set<String> knownVariables, Set<String> knownIntervalVariables) throws IOException {
      this();
      parse(solutionJson, knownVariables, knownIntervalVariables);
    }

    public void reset() {
      feasible = false;
      name2val = new HashMap<String, Double>();
      var2val = new HashMap<IloNumVar, Double>();

      name2start = new HashMap<String, Double>();
      var2start = new HashMap<IloIntervalVar, Double>();

      name2size = new HashMap<String, Double>();
      var2size = new HashMap<IloIntervalVar, Double>();

      name2end = new HashMap<String, Double>();
      var2end = new HashMap<IloIntervalVar, Double>();
      /*
      objective = Double.NaN;
      status = -1;
      pfeas = false;
      dfeas = false;
      */
    }

    public void parse(JSONObject solutionJson, Set<String> knownVariables, Set<String> knownIntervalVariables) throws IOException {
      reset();

      feasible = solutionJson.getJSONObject("solutionStatus").getString("solveStatus").equals("Feasible");
      if (solutionJson.has("intVars")) {
        JSONObject intVars = solutionJson.getJSONObject("intVars");
        for (Iterator<String> it = intVars.keySet().iterator(); it.hasNext(); ) {
          String name = it.next();
          Integer value = ((Integer) intVars.get(name));
          name2val.put(name, value.doubleValue());
        }
      }

      if (solutionJson.has("intervalVars")) {
        JSONObject intervalVars = solutionJson.getJSONObject("intervalVars");
        for (Iterator<String> it = intervalVars.keySet().iterator(); it.hasNext(); ) {
          String name = it.next();
          double start = intervalVars.getJSONObject(name).getDouble("start");
          name2start.put(name, start);
          double size = intervalVars.getJSONObject(name).getDouble("size");
          name2size.put(name, size);
          double end = intervalVars.getJSONObject(name).getDouble("end");
          name2end.put(name, end);
        }
      }

    }
  }


  private Solution result = null;

  public boolean solve() throws IloException {

    final HashMap<String,IloNumVar> intVars = new HashMap<String,IloNumVar>();
    IloIntVarArray allIntVars = getCPImpl().getAllIloIntVars();
    for (int  i=0; i<allIntVars.getSize(); i++) {
      IloIntVar v = allIntVars.getIntVar(i);
      if (v.getName() == null) {
        v.setName("IntVar_" + i);
      }
      intVars.put(v.getName(), v);
    }

    final HashMap<String,IloIntervalVar> intervalVars = new HashMap<String,IloIntervalVar>();
    IloIntervalVarArray allIntervalVars = getCPImpl().getAllIloIntervalVars();
    for (int  i=0; i<allIntervalVars.getSize(); i++) {
      IloIntervalVar v = allIntervalVars.get_IloIntervalVar(i);
      if (v.getName() == null) {
        v.setName("IntervalVar_" + i);
      }
      intervalVars.put(v.getName(), v);
    }

    // Now perform the solve
    result = externalSolve(intVars.keySet(), intervalVars.keySet());

    // Transfer non-zeros indexed by name to non-zeros indexed by object.
    for (final Map.Entry<String, Double> e : result.name2val.entrySet()) {
      result.var2val.put(intVars.get(e.getKey()), e.getValue());
    }
    result.name2val.clear();

    // Transfer non-zeros indexed by name to non-zeros indexed by object.
    for (final Map.Entry<String, Double> e : result.name2start.entrySet()) {
      result.var2start.put(intervalVars.get(e.getKey()), e.getValue());
    }
    result.name2start.clear();

    // Transfer non-zeros indexed by name to non-zeros indexed by object.
    for (final Map.Entry<String, Double> e : result.name2size.entrySet()) {
      result.var2size.put(intervalVars.get(e.getKey()), e.getValue());
    }
    result.name2size.clear();

    // Transfer non-zeros indexed by name to non-zeros indexed by object.
    for (final Map.Entry<String, Double> e : result.name2end.entrySet()) {
      result.var2end.put(intervalVars.get(e.getKey()), e.getValue());
    }
    result.name2end.clear();


    return result.feasible;

  }

  /** Perform an external solve.
   */
  protected abstract Solution externalSolve(Set<String> variables, Set<String> intervalVariables) throws IloException;
  
  // Below we overwrite a bunch of IloCplex functions that query solutions.
  // Add your own overwrites if you need more.
  
  @Override
  public double getObjValue() throws IloException {
    if (result == null)
      throw new IloException("No solution available");
    return 0; // TODO
  }
  
  @Override
  public double getValue(IloNumVar v)  {

    if (result == null)
      return 0; // TODO CHECK
    if (result.var2val.containsKey(v))
      return result.var2val.get(v);
    else
      return 0; // TODO CHECK
  }

  @Override
  public boolean isPresent(IloIntervalVar a) {
    if (result == null)
      return false; // TODO CHECK
    if (result.var2start.containsKey(a))
      return true;
    else
      return false; // TODO CHECK
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
}
