package uk.ac.ebi.biosd.xs.service;

import java.util.Date;

import uk.ac.ebi.biosd.xs.export.AGE1XMLFormatter;
import uk.ac.ebi.biosd.xs.export.AGE2GSXMLFormatter;
import uk.ac.ebi.biosd.xs.export.AGE2XMLFormatter;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.STXMLFormatter;

public class SchemaManager
{
 public static final String STXML = "ST";
 public static final String AGEXML1 = "AGE1";
 public static final String AGEXML2GS = "AGE2GS";
 public static final String AGEXML2 = "AGE2";
 

 public static AbstractXMLFormatter getFormatter( String name, boolean showAttributes, boolean showAC, SamplesFormat smpfmt, boolean pubOnly, Date now )
 {
  
  if( STXML.equals(name) )
   return new STXMLFormatter( showAttributes, showAC, smpfmt, pubOnly, now);
  else if( AGEXML1.equals(name) )
   return new AGE1XMLFormatter( showAttributes, showAC, smpfmt, pubOnly, now);
  else if( AGEXML2.equals(name) )
   return new AGE2XMLFormatter( showAttributes, showAC, smpfmt, pubOnly, now);
  else if( AGEXML2GS.equals(name) )
   return new AGE2GSXMLFormatter( showAttributes, showAC, smpfmt, pubOnly, now);
  
  return null;
 }
}
