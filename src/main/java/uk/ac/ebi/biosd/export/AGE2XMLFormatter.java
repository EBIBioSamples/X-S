package uk.ac.ebi.biosd.export;

import java.io.IOException;

public class AGE2XMLFormatter extends AGE1XMLFormatter
{
 private static String nameSpace = "http://www.ebi.ac.uk/biosamples/SampleGroupExportV2";

 public AGE2XMLFormatter(boolean showNS, boolean showAttributes, boolean showAC, SamplesFormat smpfmt)
 {
  super(showNS, showAttributes, showAC, smpfmt);
 }

 
 @Override
 protected String getNameSpace()
 {
  return nameSpace;
 }

 @Override
 protected void exportSimpleValuePefix( Appendable out ) throws IOException
 {
  out.append("<simpleValue>");
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
  out.append("<objectValue>");
 }

 @Override
 protected void exportObjectValuePostfix( Appendable out ) throws IOException
 {
  out.append("</objectValue>\n");
 }
 
}
