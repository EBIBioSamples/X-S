package uk.ac.ebi.biosd.xs.mtexport;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public class FormattingRequest
{
 private final XMLFormatter          formatter;
 private final Appendable groupOut;
 private final Appendable sampleOut;
  
 public FormattingRequest(XMLFormatter formatter, Appendable groupOut, Appendable sampleOut)
 {
  super();
  this.formatter = formatter;
  this.groupOut = groupOut;
  this.sampleOut = sampleOut;
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
