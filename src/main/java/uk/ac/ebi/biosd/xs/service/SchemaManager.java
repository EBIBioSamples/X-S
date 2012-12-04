package uk.ac.ebi.biosd.xs.service;

import java.util.Map;
import java.util.TreeMap;

import uk.ac.ebi.biosd.export.AGE1XMLFormatter;
import uk.ac.ebi.biosd.export.AGE2XMLFormatter;
import uk.ac.ebi.biosd.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.export.STXMLFormatter;

public class SchemaManager
{
 public static final String STXML = "ST";
 public static final String AGEXML1 = "AGE1";
 public static final String AGEXML2 = "AGE2";
 
 private static Map<String, AbstractXMLFormatter> fmtMap;
 
 static
 {
  fmtMap = new TreeMap<String, AbstractXMLFormatter>();
  
  fmtMap.put(STXML, new STXMLFormatter());
  fmtMap.put(AGEXML1, new AGE1XMLFormatter());
  fmtMap.put(AGEXML2, new AGE2XMLFormatter());
 }
 
 public static AbstractXMLFormatter getFormatter( String name )
 {
  return fmtMap.get(name);
 }
}
