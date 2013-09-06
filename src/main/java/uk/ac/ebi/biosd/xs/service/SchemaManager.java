package uk.ac.ebi.biosd.xs.service;

import uk.ac.ebi.biosd.xs.export.AGE1GSXMLFormatter;
import uk.ac.ebi.biosd.xs.export.AGE1XMLFormatter;
import uk.ac.ebi.biosd.xs.export.AGE2XMLFormatter;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.STXMLFormatter;

public class SchemaManager
{
 public static final String STXML = "ST";
 public static final String AGEXML1 = "AGE1";
 public static final String AGEXML1GS = "AGE1GS";
 public static final String AGEXML2 = "AGE2";
 

 public static AbstractXMLFormatter getFormatter( String name, boolean showNS, boolean showAttributes, boolean showAC, SamplesFormat smpfmt )
 {
  if( STXML.equals(name) )
   return new STXMLFormatter(showNS, showAttributes, showAC, smpfmt);
  else if( AGEXML1.equals(name) )
   return new AGE1XMLFormatter(showNS, showAttributes, showAC, smpfmt);
  else if( AGEXML2.equals(name) )
   return new AGE2XMLFormatter(showNS, showAttributes, showAC, smpfmt);
  else if( AGEXML1GS.equals(name) )
   return new AGE1GSXMLFormatter(showNS, showAttributes, showAC, smpfmt);
  
  return null;
 }
}
