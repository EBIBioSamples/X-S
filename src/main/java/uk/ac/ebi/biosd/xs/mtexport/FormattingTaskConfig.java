package uk.ac.ebi.biosd.xs.mtexport;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public class FormattingTaskConfig
{
 private final XMLFormatter          formatter;
 private final Appendable groupOut;
 private final Appendable sampleOut;
 private final boolean groupedSamplesOnly;
  
 public FormattingTaskConfig(XMLFormatter formatter, Appendable groupOut, Appendable sampleOut, boolean grpOnly )
 {
  super();
  this.formatter = formatter;
  this.groupOut = groupOut;
  this.sampleOut = sampleOut;
  groupedSamplesOnly = grpOnly;
 }

 public XMLFormatter getFormatter()
 {
  return formatter;
 }

 public Appendable getGroupOut()
 {
  return groupOut;
 }

 public Appendable getSampleOut()
 {
  return sampleOut;
 }

}
