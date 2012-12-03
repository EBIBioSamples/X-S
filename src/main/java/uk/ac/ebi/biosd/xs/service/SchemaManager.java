package uk.ac.ebi.biosd.xs.service;

import java.util.Map;
import java.util.TreeMap;

import uk.ac.ebi.biosd.export.AGEXMLFormatter;
import uk.ac.ebi.biosd.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.export.STXMLFormatter;

public class SchemaManager
{
 public static final String STXML = "ST";
 public static final String AGEXML = "AGE";
 
 private static Map<String, AbstractXMLFormatter> fmtMap;
 
 static
 {
  fmtMap = new TreeMap<String, AbstractXMLFormatter>();
  
  fmtMap.put(STXML, new STXMLFormatter());
  fmtMap.put(AGEXML, new AGEXMLFormatter());
 }
 
 public static AbstractXMLFormatter getFormatter( String name )
 {
  return fmtMap.get(name);
 }
}
