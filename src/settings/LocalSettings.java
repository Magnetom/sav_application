package settings;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;

public class LocalSettings {

    private static Document document;
    private static XPathFactory pathFactory ;
    private static XPath xpath;

    static {
        try {

            document    = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("settings.xml");
            pathFactory = XPathFactory.newInstance();
            xpath       = pathFactory.newXPath();

        }catch (ParserConfigurationException | SAXException | IOException ex) {
            document = null;
            ex.printStackTrace(System.out);
        }
    }

    private static String getXMLValue(String address, String name) throws XPathExpressionException, ParserConfigurationException{

        if (document == null)    throw new ParserConfigurationException("Document is null pointer!");
        if (pathFactory == null) throw new XPathExpressionException("XPathFactory instance is a null pointer!");
        if (xpath == null)       throw new XPathExpressionException("XPath instance is a null pointer!");

        XPathExpression expr = xpath.compile(address+"/"+name);
        NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        return nodes.item(0).getTextContent();
    }

    private static long getXMLValueLong(String address, String name, String defaultValue) {
        String value = (defaultValue!=null)?defaultValue:"0";
        try {
            value = getXMLValue(address, name);
        } catch (XPathExpressionException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        return Long.valueOf(value);
    }

    private static String getXMLValueString(String address, String name, String defaultValue) {
        String value = (defaultValue!=null)?defaultValue:"";
        try {
            value = getXMLValue(address, name);
        } catch (XPathExpressionException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        return value;
    }


    public static void reloadSettings(){

        CachedSettings.SERVER_ADDRESS = getXMLValueString("settings/network", "server_address", CachedSettings.DEFAULT_SERVER_ADDRES);

    }
}
