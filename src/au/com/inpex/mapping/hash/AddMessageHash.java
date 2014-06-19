package au.com.inpex.mapping.hash;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;


/**
 * Add a field containing a hash of the text contents of the message.
 * 
 * A mapping parameter is used to specify a top-level message node from
 * which all the text content of sub-nodes are hashed.
 * The resulting hash is added as a new field: "hash".
 * 
 */
public class AddMessageHash extends AbstractTransformation {

	@Override
	public void transform(TransformationInput in, TransformationOutput out)
		throws StreamTransformationException {

		InputStream messageInputstream = in.getInputPayload().getInputStream();
		OutputStream messageOutputStream = out.getOutputPayload().getOutputStream();
		String topLevelNode = in.getInputParameters().getString("TOP_LEVEL_NODE");
		
		// DOM processing
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document = builder.parse(messageInputstream);
			
			NodeList nodes = document.getElementsByTagName(topLevelNode);
			if (nodes.getLength() == 0) {
				getTrace().addInfo("top-level-node not found!");
			} else {
				Node node = nodes.item(0);
				String textContent = node.getTextContent();
				getTrace().addInfo("************************************");
				getTrace().addInfo(textContent);
				
				String md5 = DigestUtils.md5Hex(textContent);
				Element newHashElement = document.createElement("hash");
				newHashElement.appendChild(document.createTextNode(md5));
				
				node.appendChild(newHashElement);
			}
			
			DOMSource source = new DOMSource(document);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StreamResult streamResult = new StreamResult(messageOutputStream);
			transformer.transform(source, streamResult);
			
		} catch (Exception e) {
			getTrace().addInfo("AddMessageHash MAPPING ERROR: " + e.getMessage()); 
		}
	}

}
