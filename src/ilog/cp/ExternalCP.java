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


  private JSONObject result = null;

  public boolean solve() throws IloException {
    result = null;

    IloIntVarArray intVars = getCPImpl().getAllIloIntVars();
    for (int  i=0; i<intVars.getSize(); i++) {
      if (intVars.getIntVar(i).getName() == null) {
        intVars.getIntVar(i).setName("IntVar_" + i);
      }
    }

    // Now perform the solve
    result = externalSolve();

    return result != null;

  }

  /** Perform an external solve.
   */
  protected abstract JSONObject externalSolve() throws IloException;
  
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
    try {
      if (v.getType() == IloNumVarType.Int) {
        String name = v.getName();
        JSONObject intvars = result.getJSONObject("intVars");
        Integer value = ((Integer)intvars.get(name));
        return value.doubleValue();

      }
    } catch (IloException e) {
      return 0;
    }

    return 0;
  }
  


}
