package com.xmlparsing.charter;

/**
 *@author YOGESH KUMAR
 *@ykumar10
 *10/09/2017
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.functions.Count;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CharterDaynamicInputXml {
	public static void main(String[] args) throws IOException {
		Map<String, String> propsKVMap = new LinkedHashMap<String, String>();
		propsKVMap.put("CMMAC", "10100000BB67");
		propsKVMap.put("ServiceId", "88888888");
		propsKVMap.put("TCName", "NewConnect");
		propsKVMap.put("EndSystem", "MacLogging");
		propsKVMap.put("APIVersion", "1.0");
		propsKVMap.put("ServiceType", "HSD");
		propsKVMap.put("Reconnect", "NO");
		propsKVMap.put("Transfer", "YES");
		propsKVMap.put("Flavour", "DYNAMIC");
		propsKVMap.put("accountType", "RES");
		propsKVMap.put("logDir", "D:\\ExcelFormat.xls");
		String ResultProps = PropXMLString(propsKVMap);
		String sodiXmlFilePath = "D:\\MultipalHsdSODIXMLSWAP.xml";
		StringBuilder sodiXMLString = convertXmlToString(sodiXmlFilePath);
		String s = createDynamicValidationXML(sodiXMLString.toString(),
				ResultProps);
        System.out.println(s);
	}

	public static String createDynamicValidationXML(String sodiXMLString,
			String propertiesXML) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		DocumentBuilder propbuilder;
		String accountType = null;
		String accountId = null;
		String customerId = null;
		String dynamicXml = null;
		String flag = "SIDNOTFOUND";
		int i = 0;
		Map<String, String> responseMap = new LinkedHashMap<String, String>();
		try {
			builder = factory.newDocumentBuilder();
			propbuilder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(
					sodiXMLString.toString())));
			factory.setNamespaceAware(true);
			Document propertiesDocument = propbuilder.parse(new InputSource(
					new StringReader(propertiesXML.toString())));

			NodeList propNodeList = propertiesDocument
					.getElementsByTagName("Property");
			Map<String, String> responsePropMap = new LinkedHashMap<String, String>();

			for (int j = 0; j < propNodeList.getLength(); j++) {
				for (int k = 0; k < 1; k++) {

					responsePropMap.put(propNodeList.item(j).getChildNodes()
							.item(k + 1).getFirstChild().getNodeValue(),
							propNodeList.item(j).getChildNodes().item(k + 3)
									.getFirstChild().getNodeValue());

				}
			}
			if (document.getElementsByTagName("ItemList").getLength() != 0) {
				NodeList nodeList = document.getElementsByTagName("Item");
				accountType = document.getElementsByTagName("Type").item(0)
						.getFirstChild().getNodeValue();
				accountId = document.getElementsByTagName("AccountId").item(0)
						.getFirstChild().getNodeValue();
				customerId = document.getElementsByTagName("CustomerId")
						.item(0).getFirstChild().getNodeValue();
				for (i = 0; i < nodeList.getLength(); i++) {
					int k = 1;
					if (nodeList.item(i).getChildNodes().item(k)
							.getFirstChild().getNodeValue()
							.equalsIgnoreCase(responsePropMap.get("SERVICEID"))) {
						for (int j = 1; j < nodeList.item(i).getChildNodes()
								.getLength(); j += 2) {
							if (nodeList.item(i).getChildNodes().item(j)
									.getNodeName()
									.contentEquals("ServiceIdentifier")) {
								responseMap.put("ServiceIdentifier" + i,
										nodeList.item(i).getChildNodes()
												.item(j).getFirstChild()
												.getNodeValue());
							}
							if (nodeList.item(i).getChildNodes().item(j)
									.getNodeName().contentEquals("Action")) {
								responseMap.put("Action" + i, nodeList.item(i)
										.getChildNodes().item(j)
										.getFirstChild().getNodeValue());
							}

						}

						dynamicXml = generateDaynamicInputXmlNode(i,
								responseMap, customerId, accountId,
								accountType, responsePropMap);
						flag = "SIDFOUND";
					}
					if (flag.equals("SIDFOUND")) {
						break;
					}

				}
				if (flag.equals("SIDNOTFOUND")) {
					return "  ServiceID is Not Present at Requested SODIXML";
				}
			} else {
				int initialActionItem = 0;
				String actionType = document.getElementsByTagName("ActionType")
						.item(0).getFirstChild().getNodeValue();
				String serviceId = document.getElementsByTagName("ServiceId")
						.item(0).getFirstChild().getNodeValue();
				String calServiceId = calculateServiceID(serviceId);

				if (calServiceId.equalsIgnoreCase(responsePropMap
						.get("SERVICEID")) && calServiceId != null) {

					if (responsePropMap.containsKey("ACCOUNTTYPE")) {
						accountType = responsePropMap.get("ACCOUNTTYPE");
					}
					accountId = document.getElementsByTagName("AccountId")
							.item(0).getFirstChild().getNodeValue();
					customerId = document.getElementsByTagName("CustomerId")
							.item(0).getFirstChild().getNodeValue();
					responseMap.put("Action0", actionType);
					dynamicXml = generateDaynamicInputXmlNode(
							initialActionItem, responseMap, customerId,
							accountId, accountType, responsePropMap);
				}

				else {
					return "  ServiceID is Not Present at Requested XML";
				}

			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		return dynamicXml;
	}

	private static String generateDaynamicInputXmlNode(int i,
			Map<String, String> responseMap, String customerId,
			String accountId, String accountType,
			Map<String, String> responsePropMap) {
		Document doc = null;
		StreamResult result = null;
		DOMSource source = null;
		String strResult = null;

		try {

			DocumentBuilderFactory icFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder icBuilder;
			icBuilder = icFactory.newDocumentBuilder();
			doc = icBuilder.newDocument();
			Element mainRootElement = doc.createElement("Validate");
			doc.appendChild(mainRootElement);
			Element Scenario = doc.createElement("Scenario");
			if (responsePropMap.containsKey("TCNAME")) {
				Scenario.appendChild(doc.createTextNode(responsePropMap
						.get("TCNAME")));
			}
			mainRootElement.appendChild(Scenario);
			// Second elements
			Element apiName = doc.createElement("APIName");
			if (responsePropMap.containsKey("ENDSYSTEM")) {
				apiName.appendChild(doc.createTextNode(responsePropMap
						.get("ENDSYSTEM")));
			}
			mainRootElement.appendChild(apiName);

			// Third elements
			Element apiVersion = doc.createElement("APIVersion");
			if (responsePropMap.containsKey("APIVERSION")) {
				apiVersion.appendChild(doc.createTextNode(responsePropMap
						.get("APIVERSION")));
			}
			mainRootElement.appendChild(apiVersion);
			// Fourth elements
			Element customerID = doc.createElement("CustomerID");
			customerID.appendChild(doc.createTextNode(customerId));
			mainRootElement.appendChild(customerID);
			// Fifth elements
			Element accountID = doc.createElement("AccountID");
			accountID.appendChild(doc.createTextNode(accountId));
			mainRootElement.appendChild(accountID);
			// Sixth elements
			Element acType = doc.createElement("AccountType");
			if (accountType != null) {
				acType.appendChild(doc.createTextNode(accountType));
			}
			mainRootElement.appendChild(acType);
			createServiceNode(doc, mainRootElement, i, accountId, responseMap,
					responsePropMap);
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			source = new DOMSource(doc);

			StringWriter writer = new StringWriter();
			result = new StreamResult(writer);
			transformer.transform(source, result);
			strResult = writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return strResult;
	}

	// utility method to create text node
	private static void createServiceNode(Document doc,
			Element mainRootElement, int j, String accountId,
			Map<String, String> responseMap, Map<String, String> responsePropMap) {
		// Service Element
		Element service = doc.createElement("Service");
		mainRootElement.appendChild(service);

		Element serviceID = doc.createElement("ServiceID");
		if (responsePropMap.containsKey("SERVICEID")) {
			serviceID.appendChild(doc.createTextNode(responsePropMap
					.get("SERVICEID")));
		}
		service.appendChild(serviceID);

		Element serviceTyp = doc.createElement("ServiceType");
		if (responsePropMap.containsKey("SERVICETYPE")) {
			serviceTyp.appendChild(doc.createTextNode(responsePropMap
					.get("SERVICETYPE")));
		}
		service.appendChild(serviceTyp);

		Element flavour = doc.createElement("Flavour");
		if (responsePropMap.containsKey("FLAVOUR")) {
			flavour.appendChild(doc.createTextNode(responsePropMap
					.get("FLAVOUR")));
		}
		service.appendChild(flavour);

		Element action = doc.createElement("Action");
		action.appendChild(doc.createTextNode(responseMap.get("Action" + j)));
		service.appendChild(action);

		Element transfer = doc.createElement("Transfer");
		if (responsePropMap.containsKey("TRANSFER")) {
			transfer.appendChild(doc.createTextNode(responsePropMap
					.get("TRANSFER")));
		}
		service.appendChild(transfer);

		Element reconnect = doc.createElement("Reconnect");
		if (responsePropMap.containsKey("RECONNECT")) {
			reconnect.appendChild(doc.createTextNode(responsePropMap
					.get("RECONNECT")));
		}
		service.appendChild(reconnect);
		// Read The Excel File and return the MapResponse
		LinkedHashMap<String, String> excelValidationResponseMap = readExcelInputValidationValues(responsePropMap);
		// Validation Node Inside Service Element
		if(excelValidationResponseMap.size()!=0)
		{
		Element validations = doc.createElement("DynamicValidation");
		service.appendChild(validations);
		System.out.println(excelValidationResponseMap.get("MapCounter"));
		for (int i = 1; i <= Integer.parseInt(excelValidationResponseMap.get("MapCounter")); i++) {
			validationBlock(i, doc, validations, accountId,
					excelValidationResponseMap, responsePropMap);

		}
		
		}
		// Resources Inside Service Element
		Element resources = doc.createElement("Resources");
		service.appendChild(resources);

		Element resource = doc.createElement("Resource");
		resources.appendChild(resource);

		if (responsePropMap.containsKey("CMMAC")) {
			Element name = doc.createElement("Name");
			name.appendChild(doc.createTextNode("CMMAC"));
			resource.appendChild(name);

			Element type = doc.createElement("Type");
			type.appendChild(doc.createTextNode(responsePropMap.get("CMMAC")));
			resource.appendChild(type);
		}
		if (responseMap.get("Action" + j).equalsIgnoreCase("ADD")) {
			String rootMapAction = responseMap.get("Action" + j);
			Element rcAction = doc.createElement("Action");
			rcAction.appendChild(doc.createTextNode(rootMapAction));
			resource.appendChild(rcAction);
		}

	}

	// Method to Create Properties File.
	public static String PropXMLString(Map<String, String> propsKVMap) {
		String strResult = null;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document = builder.newDocument();
			Element Properties = document.createElement("Properties");
			document.appendChild(Properties);
			Iterator<Entry<String, String>> iterator = propsKVMap.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, String> pair = (Map.Entry<String, String>) iterator
						.next();
				appendPropElement(document, Properties, pair.getKey()
						.toUpperCase(), pair.getValue());
			}
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(document);
			// Converting to String from Stream Object
			StringWriter writer = new StringWriter();
			StreamResult result1 = new StreamResult(writer);
			transformer.transform(source, result1);
			strResult = writer.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return strResult;
	}

	private static void appendPropElement(Document document,
			Element Properties, String key, String value) {

		Element property = document.createElement("Property");
		Properties.appendChild(property);

		Element propertyName = document.createElement("Name");
		propertyName.appendChild(document.createTextNode(key));
		property.appendChild(propertyName);

		Element propertyValue = document.createElement("Value");
		propertyValue.appendChild(document.createTextNode(value));
		property.appendChild(propertyValue);

	}

	public static String calculateServiceID(String stringValue) {
		String dyValue = null;
		char str[] = stringValue.toCharArray();
		for (int i = stringValue.length() - 1; i > 0; i--) {
			if (str[i] == '_') {
				dyValue = stringValue.substring(i + 1, stringValue.length());
				break;
			}
		}
		return dyValue;
	}

	public static StringBuilder convertXmlToString(String xmlFileLocation)
			throws IOException {
		@SuppressWarnings("resource")
		BufferedReader bufferedReader = new BufferedReader(new FileReader(
				new File(xmlFileLocation)));
		String line;
		StringBuilder sb = new StringBuilder();

		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line.trim());
		}

		return sb;

	}

	public static void validationBlock(int i, Document doc,
			Element validations, String accountId,
			LinkedHashMap<String, String> validationResponse,
			Map<String, String> propResponse) {

		Element validationBlock = doc.createElement("ValidationBlock");
		validations.appendChild(validationBlock);
		if (validationResponse.containsKey("AttributeName" + i)) {

			Element attributeName = doc.createElement("AttributeName");
			attributeName.appendChild(doc.createTextNode(validationResponse
					.get("AttributeName" + i)));
			validationBlock.appendChild(attributeName);

			if (validationResponse.get("AttributeName" + i).equals(
					"AccountNumber")) {
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc.createTextNode(accountId));
				validationBlock.appendChild(expectedValue);
			}
			if (validationResponse.get("AttributeName" + i)
					.equals("MacAddress")) {
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc.createTextNode(propResponse
						.get("CMMAC")));
				validationBlock.appendChild(expectedValue);
			}
			if (validationResponse.get("AttributeName" + i).equals("EndDate")) {
				
				Date date = new Date();
				//With Time Stamp
				//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String charterRequestDateFormat = sdf.format(date);
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc
						.createTextNode(charterRequestDateFormat));
				validationBlock.appendChild(expectedValue);
			}
			
			
		if (validationResponse.get("AttributeName" +i)
					.equals("LogEntryStamp")) {
			System.out.println("HI");
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc.createTextNode("---"));
				validationBlock.appendChild(expectedValue);
			}
			
			if (validationResponse.get("AttributeName" + i)
					.equals("Action")) {
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc.createTextNode("Action Value"));
				validationBlock.appendChild(expectedValue);
			}
			
			if (validationResponse.get("AttributeName" + i)
					.equals("Service")) {
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc.createTextNode(("SERVICEIDVALUE")));
				validationBlock.appendChild(expectedValue);
			}
			
			if (validationResponse.get("AttributeName" + i)
					.equals("LogEntryText")) {
				Element expectedValue = doc.createElement("ExpectedValue");
				expectedValue.appendChild(doc.createTextNode(("--LogEntryTextValue---")));
				validationBlock.appendChild(expectedValue);
			}
			

		}

		if (validationResponse.containsKey("Xpath" + i)) {

			Element xPath = doc.createElement("XPath");
			xPath.appendChild(doc.createTextNode(validationResponse.get("Xpath"
					+ i)));
			validationBlock.appendChild(xPath);

		}

	}

	public static LinkedHashMap<String, String> readExcelInputValidationValues(
			Map<String, String> propResponseMap) {
		LinkedHashMap<String, String> validationResponseMap = new LinkedHashMap<String, String>();
		Workbook workbookFectory = null;
		Row headerRow = null;
		
		if(propResponseMap.containsKey("LOGDIR"))
		{
			
			String filePath = propResponseMap.get("LOGDIR");
			File validationFile = new File(filePath);
			int count=1;
			try {
				if (validationFile.exists() && validationFile.isFile()) {

					FileInputStream file = new FileInputStream(validationFile);

					workbookFectory = WorkbookFactory.create(file);
					Sheet updateSheet = workbookFectory
							.getSheet("DynamicValidation");
					headerRow = updateSheet.getRow(0);
					headerRow.getLastCellNum();
					System.out.println(headerRow.getLastCellNum());
					for (int i = 1; i <= updateSheet.getLastRowNum(); i++) {
						Row startContentRow = updateSheet.getRow(i);

						if (startContentRow.getCell(0).getStringCellValue()
								.equals(propResponseMap.get("ENDSYSTEM"))) {
							for (int j = 0; j <startContentRow.getLastCellNum(); j++) {
								String firstColmValue = startContentRow.getCell(j)
										.getStringCellValue();
								System.out.println("firstColmValue"+firstColmValue);

								validationResponseMap.put(headerRow.getCell(j)
										.getStringCellValue() + count, firstColmValue);
								
							}
							count++;
							
						}

					}
                
				}
			} catch (EncryptedDocumentException e) {
				e.printStackTrace();
			} catch (InvalidFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		    count=count-1;
			validationResponseMap.put("MapCounter", Integer.toString(count));
		}
		
		System.out.println(validationResponseMap);
		System.out.println(validationResponseMap.size());
		
		return validationResponseMap;
	}

	public static void writeSODIXMLSOAPObjectTodirPath(String xmlString,
			String filePath) {
		PrintWriter printWriter = null;

		try {
			printWriter = new PrintWriter(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		printWriter.println(xmlString);
		printWriter.close();
	}

}
