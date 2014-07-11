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

import au.com.inpex.mapping.hash.exceptions.ParameterMissingException;

import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;
import com.sap.aii.mapping.api.UndefinedParameterException;


/**
 * Add a field containing a hash of the text contents of the message.
 * 
 * Mapping Parameters:
 * 
 * TOP_LEVEL_NODE - This is the node the new hash field will be placed
 * inside
 * HASH_FIELD_NAME - This is the name of the new hash element
 * PAYLOAD_FIELD_NAME - This is the name of the nodeset that will be
 * hashed (all text elements within the node makeup the hash)
 * DELETE_PAYLOAD_FIELD - If this is TRUE then the specified payload
 * field is deleted from the message.
 * 
 */
public class AddMessageHash extends AbstractTransformation {

	@Override
	public void transform(TransformationInput in, TransformationOutput out)
		throws StreamTransformationException {

		InputStream messageInputstream = in.getInputPayload().getInputStream();
		OutputStream messageOutputStream = out.getOutputPayload().getOutputStream();

		// Read in mapping parameters, providing defaults for the hash field name
		// and delete payload indicator.
		String topLevelNodeName = in.getInputParameters().getString("TOP_LEVEL_NODE");
		String payloadFieldName = in.getInputParameters().getString("PAYLOAD_FIELD_NAME");
		boolean deletePayloadField = false;
		try {
			deletePayloadField = in.getInputParameters().getString("DELETE_PAYLOAD_FIELD").equalsIgnoreCase("true")? true : false;
		} catch (UndefinedParameterException e) { }
		String hashNodeName= "";
		try {
			hashNodeName = in.getInputParameters().getString("HASH_FIELD_NAME");
		} catch (UndefinedParameterException e) {
			hashNodeName = "Hash";
		}
		
		// DOM processing
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document = builder.parse(messageInputstream);
			
			NodeList topLevelNodeSet = document.getElementsByTagName(topLevelNodeName);
			if (topLevelNodeSet.getLength() == 0) {
				throw new ParameterMissingException("TOP_LEVEL_NODE parameter not found!");
			}
			
			NodeList payloadNodeSet = document.getElementsByTagName(payloadFieldName);
			if (payloadNodeSet.getLength() == 0) {
				throw new ParameterMissingException("PAYLOAD_FIELD_NAME parameter not found!");
			}
			
			// get the text content of the payload and hash it
			Node payloadNode = payloadNodeSet.item(0);
			String textContent = payloadNode.getTextContent();
			getTrace().addInfo("************************************");
			getTrace().addInfo(textContent);
			
			String md5 = DigestUtils.md5Hex(textContent);

			// create a new hash element and add it to the document 
			Element newHashElement = document.createElement(hashNodeName);
			newHashElement.appendChild(document.createTextNode(md5));
			Node topLevelNode = topLevelNodeSet.item(0);
			topLevelNode.appendChild(newHashElement);
			
			// delete the specified payload field if required
			if (deletePayloadField) {
				Node parent = payloadNode.getParentNode();
				parent.removeChild(payloadNode);
			}
			
			// transform the xml document to the output stream
			DOMSource source = new DOMSource(document);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StreamResult streamResult = new StreamResult(messageOutputStream);
			transformer.transform(source, streamResult);
			
		} catch (ParameterMissingException p) {
			getTrace().addInfo("AddMessageHash MAPPING ERROR: " + p.getMessage());
			throw new StreamTransformationException("Mapping Parameter error", p);
		} catch (Exception e) {
			throw new StreamTransformationException("XML document processing error", e);
		}
	}

}
