package uk.ac.ebi.biosd.xs.export;

import java.io.IOException;
import java.util.Date;

public class AGE2XMLFormatter extends AGE1XMLFormatter
{
 private static String nameSpace = "http://www.ebi.ac.uk/biosamples/SampleGroupExportV2";

 public AGE2XMLFormatter( boolean showAttributes, boolean showAC, SamplesFormat smpfmt, boolean pubOnly, Date now, String eqExcl)
 {
  super( showAttributes, showAC, smpfmt, pubOnly, now, eqExcl );
 }

 
 @Override
 protected String getNameSpace()
 {
  return nameSpace;
 }

 @Override
 protected void exportSimpleValuePefix( Appendable out ) throws IOException
 {
  out.append("<simpleValue>\n");
 }

 @Override
 protected void exportSimpleValuePostfix( Appendable out ) throws IOException
 {
  out.append("</simpleValue>\n");
 }
 
 @Override
 protected void exportSimpleValueStringPefix( Appendable out ) throws IOException
 {
  out.append("<value>");
 }

 @Override
 protected void exportSimpleValueStringPostfix( Appendable out ) throws IOException
 {
  out.append("</value>\n");
 }
 
 @Override
 protected void exportObjectValuePrefix( Appendable out ) throws IOException
 {
  out.append("<objectValue>\n");
 }

 @Override
 protected void exportObjectValuePostfix( Appendable out ) throws IOException
 {
  out.append("</objectValue>\n");
 }
 
}
