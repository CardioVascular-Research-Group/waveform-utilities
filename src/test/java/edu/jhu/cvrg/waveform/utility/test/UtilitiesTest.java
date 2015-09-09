package edu.jhu.cvrg.waveform.utility.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.junit.Test;

import edu.jhu.cvrg.waveform.utility.AnalysisInProgress;
import edu.jhu.cvrg.waveform.utility.ServerUtility;
import edu.jhu.cvrg.waveform.utility.WebServiceUtility;
import junit.framework.TestCase;

public class UtilitiesTest extends TestCase{
	
	private Properties properties = new Properties();
	private String serviceURL = "";
	private String serviceName = "";
	private String serviceMethod = "";
	
    public UtilitiesTest(String testName)
    {
        super(testName);
        initializeTests();
    }
    
    private void initializeTests(){
    	try {
    		File file = new File(getClass().getResource("/test.properties").getFile());
			InputStream inputStream = new FileInputStream(file);
			properties.load(inputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	serviceURL = properties.getProperty("service.url");
    	serviceName = properties.getProperty("service.name");
    	serviceMethod = properties.getProperty("service.method");
    }
//    
//    private OMElement createOMElement(){// OMElement representing XML with the incoming parameters.
//    	OMFactory omFactory = OMAbstractFactory.getOMFactory();
//    	OMNamespace omNamespace = omFactory.createOMNamespace(serviceURL + File.separator + serviceName, serviceName);
//    	return omFactory.createOMElement(serviceMethod, omNamespace);
//    }
//
//    @Test
//    public void testBuildChildArray(){//WebServiceUtility
//    	System.out.println("testing child array builder");
//    	String[] children = WebServiceUtility.buildChildArray(createOMElement());
//    	assertTrue(children.length > 0);
//    }
//    
//    @Test
//    public void testExtractParams(){//WebServiceUtility
//    	Map<String, OMElement> map = WebServiceUtility.extractParams(createOMElement());
//    	assertTrue(map.size() > 0);
//    }
    
    @Test
    public void testGuessLeadName(){//ServerUtility
    	String leadName = ServerUtility.guessLeadName(3, 12);
    	System.out.println(leadName);
    	assertTrue(leadName.equals("III"));
    }
    
    @Test
    public void testGetFileCopyServiceURL(){//AnalysisInProgress
    	AnalysisInProgress analysis = new AnalysisInProgress();
    	analysis.analysisServiceURL = serviceURL + "/";
    	String result = analysis.getFileCopyServiceURL();
    	assertTrue(result.equals("http://10.162.38.222/dataTransferService"));
    }
    
    @Test
    public void testGetDataFilelistAsString(){//AnalysisInProgress
    	AnalysisInProgress analysis = new AnalysisInProgress();
    	String[] captains = {"Kirk", "Picard", "Janeway", "Sisko", "Archer"};
    	analysis.resultHandleList = captains;
    	String result = analysis.getResultFilelistAsString();
    	assertTrue(result.equals("Kirk^Picard^Janeway^Sisko^Archer^"));
    }
    
    @Test
    public void testGetResultFilelistAsString(){//AnalysisInProgress
    	AnalysisInProgress analysis = new AnalysisInProgress();
    	String[] captains = {"Kirk", "Picard", "Janeway", "Sisko", "Archer"};
    	analysis.resultHandleList = captains;
    	String result = analysis.getResultFilelistAsString();
    	assertTrue(result.equals("Kirk^Picard^Janeway^Sisko^Archer^"));
    }
    
    @Test
    public void testGetRelativePath(){//AnalysisInProgress
    	String path = "/opt/liferay/waveform/test";
    	AnalysisInProgress analysis = new AnalysisInProgress();
    	String result = analysis.getRelativePath(path);
    	assertTrue(result.equals("/opt/liferay/waveform/"));
    }
}