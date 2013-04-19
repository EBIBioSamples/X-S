package uk.ac.ebi.biosd.export;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ebi.biosd.keyword.OWLKeywordExpansion;
import uk.ac.ebi.biosd.xs.service.Counter;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

public class EBeyeXMLFormatter extends AbstractXMLFormatter
{
 static final String DESCRIPTION_PROPERTY = "Description";
 
 private final OWLKeywordExpansion expander;
 
 public EBeyeXMLFormatter(OWLKeywordExpansion exp)
 {
  super(false, false, false, null);
  
  expander=exp;
 }

 static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

 @Override
 public void exportSample(BioSample smp, Appendable out) throws IOException
 {
  if( ! smp.isPublic() )
   return;
  
  out.append("<entry id=\"");
  xmlEscaped(smp.getAcc(), out);
  out.append("\">\n<name>");
  xmlEscaped(smp.getAcc(), out);
  out.append("</name>\n<description>\n");
  
  Set<String> kw = new HashSet<String>();
  
  if( smp.getPropertyValues() != null )
  {
   for( ExperimentalPropertyValue<? extends ExperimentalPropertyType> val : smp.getPropertyValues() )
   {
    boolean isChar = ( val instanceof BioCharacteristicValue );
    boolean isComm = ( val instanceof SampleCommentValue );
    
    String pName = val.getType().getTermText();
    
    Collection<String> terms = expander.expand(pName.toLowerCase().trim(), false );
    
    if( terms != null )
     kw.addAll(terms);
    else
     kw.add(pName);
    
    String pVal = val.getTermText();
    
    terms = expander.expand(pVal.toLowerCase().trim(), DESCRIPTION_PROPERTY.equalsIgnoreCase(pName));

    if( terms != null )
     kw.addAll(terms);
    else
     kw.add(pName);

    
    if( isChar )
     xmlEscaped("Characteristic["+pName+"]",out);
    else if( isComm )
     xmlEscaped("Comment["+pName+"]",out);
    else
     xmlEscaped(pName,out);

    xmlEscaped(": "+pVal+"\n", out);
   }
  }
  
  out.append("</description>\n<keywords>\n");
  
  for( String w : kw )
   xmlEscaped(w, out);

  out.append("\n</keywords>\n</entry>\n");

  
 }

 @Override
 public void exportGroup(BioSampleGroup ao, Appendable out) throws IOException
 {
  // TODO Auto-generated method stub

 }

 @Override
 public void exportHeader(long ts, long since, Appendable out) throws IOException
 {
  Date d = new Date(ts);
  
  
  out.append("<database>\n<name>BioSamples database</name>\n<release_date>");
  out.append(df.format(d));
  out.append("</release_date>\n");

 }

 @Override
 public void exportFooter(Appendable out) throws IOException
 {
  out.append("</database>");
 }

 @Override
 public void exportSources(Map<String, Counter> srcMap, Appendable out) throws IOException
 {
  // TODO Auto-generated method stub

 }

}
