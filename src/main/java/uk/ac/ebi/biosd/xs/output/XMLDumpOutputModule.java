package uk.ac.ebi.biosd.xs.output;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public class XMLDumpOutputModule implements OutputModule
{
 private XMLFormatter formatter;
 private boolean groupedSamplesOnly;
 
 @Override
 public XMLFormatter getFormatter()
 {
  return formatter;
 }

 @Override
 public Appendable getGroupOut()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public Appendable getSampleOut()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public boolean isGroupedSamplesOnly()
 {
  return groupedSamplesOnly;
 }

}
