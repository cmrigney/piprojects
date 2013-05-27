package com.rigney.raspberrypiprojects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author cmrigney
 */
public class SimpleXmlSerializableObject<T> {
    
    
    /**
     * Creates an instance of the type.  (Type MUST implement a zero parameter constructor)
     * @param type
     * @param xml
     * @return
     * @throws Exception 
     */
    public static SimpleXmlSerializableObject CreateFromXmlString(Class type, String xml) throws Exception
    {
        SimpleXmlSerializableObject ser = (SimpleXmlSerializableObject)type.newInstance();
        
        ser.Deserialize(xml);
        
        return ser;
    }
    
    public static SimpleXmlSerializableObject CreateFromXmlString(String xml) throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = db.parse(bin);
        
        if(doc == null) return null;
        
        bin.close();
        
        Element header = doc.getDocumentElement();
        
        String className = header.getTagName();
        
        String fullName = "com.rigney.Reflection." + className;
        
        Class type = Class.forName(fullName);
        
        return SimpleXmlSerializableObject.CreateFromXmlString(type, xml);
    }
    
    public String Serialize() throws Exception
    {
        T obj = (T)this;
        Class objClass = obj.getClass();
        Field[] fields = objClass.getDeclaredFields();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        doc.setXmlStandalone(true);
        
        Element header = doc.createElement(objClass.getSimpleName());
        
        doc.appendChild(header);
        
        for(Field f : fields)
        {
            if(!Modifier.isPublic(f.getModifiers())) continue; //only do public members
            
            String fieldName = f.getName();
            //Type type = f.getGenericType();
            Object val = f.get(obj);
            Element element2 = doc.createElement(fieldName);
            element2.setTextContent(val.toString());
            header.appendChild(element2);
        }
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        
        ByteArrayOutputStream bArray = new ByteArrayOutputStream();
        
        StreamResult sr = new StreamResult(bArray);
        
        
        transformer.transform(source, sr);
        
        String result = bArray.toString("UTF-8");
        
        bArray.close();
        
        
        return result;
    }
    
    public void DeserializeFromElement(Element header) throws Exception
    {
    	T obj = (T)this;
        Class objClass = obj.getClass();
        Field[] fields = objClass.getDeclaredFields();
    	
    	for(Field f : fields)
        {
            if(!Modifier.isPublic(f.getModifiers())) continue; //only do public members
            
            String fieldName = f.getName();
            //Type type = f.getGenericType();
            Class type = f.getType();
            
            Node child = header.getElementsByTagName(fieldName).item(0);
            
            if(child == null) 
            {
                continue;
            }
            
            
            String content = child.getTextContent();
            
            Object newVal = GetValueAsType(content, type);
            
            
            f.set(obj, newVal);
        }
    }
    
    public void Deserialize(String xml) throws Exception
    {
        T obj = (T)this;
        Class objClass = obj.getClass();
        Field[] fields = objClass.getDeclaredFields();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = db.parse(bin);
        
        if(doc == null) return;
        
        bin.close();
        
        Element header = doc.getDocumentElement();
        
        //NodeList nodes = header.getChildNodes();
        
        int i = 0;
        
        for(Field f : fields)
        {
            if(!Modifier.isPublic(f.getModifiers())) continue; //only do public members
            
            String fieldName = f.getName();
            //Type type = f.getGenericType();
            Class type = f.getType();
            
            Node child = header.getElementsByTagName(fieldName).item(0);
            
            if(child == null) 
            {
                continue;
            }
            
            
            String content = child.getTextContent();
            
            Object newVal = GetValueAsType(content, type);
            
            
            f.set(obj, newVal);
        }
    }
    
    static Object GetValueAsType(String value, Class type)
    {
        if(type.isPrimitive())
        {
            if(type.equals(int.class))
            {
                int res = Integer.valueOf(value);
                return res;
            }
            else if(type.equals(double.class))
            {
                double res = Double.valueOf(value);
                return res;
            }
            else if(type.equals(float.class))
            {
                float res = Float.valueOf(value);
                return res;
            }
            else if(type.equals(byte.class))
            {
                byte res = Byte.valueOf(value);
                return res;
            }
            else if(type.equals(boolean.class))
            {
                boolean res = Boolean.valueOf(value);
                return res;
            }
            else if(type.equals(char.class))
            {
                char res = value.toCharArray()[0];
                return res;
            }
            else if(type.equals(short.class))
            {
                short res = Short.valueOf(value);
                return res;
            }
            else if(type.equals(long.class))
            {
                long res = Long.valueOf(value);
                return res;
            }
                
        }
        else if(type.equals(String.class))
        {
            return value;
        }
        //else if array
        return value;
    }
    
}

