package uk.ac.ebi.biosd.xs.export;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.biosd.xs.keyword.OWLKeywordExpansion;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;

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
 public boolean exportSample(BioSample smp, Appendable out) throws IOException
 {
  
  out.append("<entry id=\"");
  xmlEscaped(smp.getAcc(), out);
  out.append("\">\n<name>");
  xmlEscaped(smp.getAcc(), out);
  out.append("</name>\n<description>\n");
  
  Set<String> kw = new HashSet<String>();

  Matcher propNameMtch = Pattern.compile("^(characteristic|comment)\\[(.+)\\]$",Pattern.CASE_INSENSITIVE).matcher("");
  
  if( smp.getPropertyValues() != null )
  {
   for( ExperimentalPropertyValue<? extends ExperimentalPropertyType> val : smp.getPropertyValues() )
   {
    boolean isChar = ( val instanceof BioCharacteristicValue );
    boolean isComm = ( val instanceof SampleCommentValue );
    
    String pName = val.getType().getTermText().trim();
    
    propNameMtch.reset(pName);
    
    if( propNameMtch.matches() )
     pName = propNameMtch.group(2);
    
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
     kw.add(pVal);

    
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
  
  kw.remove( null );
  
  for( String w : kw )
  {
   xmlEscaped(w, out);
   out.append(' ');
  }
  
  out.append("\n</keywords>\n</entry>\n");

 
  return true;
 }
 

 @Override
 public boolean exportGroup(BioSampleGroup grp, Appendable out) throws IOException
 {
  
  out.append("<entry id=\"");
  xmlEscaped(grp.getAcc(), out);
  out.append("\">\n<name>");
  xmlEscaped(grp.getAcc(), out);
  out.append("</name>");//\n<description>\n");
  
  boolean hasDescription=false;
  
  Set<String> kw = new HashSet<String>();

  for( ExperimentalPropertyValue<? extends ExperimentalPropertyType> val : grp.getPropertyValues() )
  {
   String pVal = val.getTermText();

   
   if( DESCRIPTION_PROPERTY.equalsIgnoreCase(val.getType().getTermText()) )
   {
    if( ! hasDescription )
    {
     out.append("\n<description>\n");
     hasDescription = true;
    }
    
    xmlEscaped(pVal, out);
   }
   
   Collection<String> terms = expander.expand(pVal.trim(), true );
   
   if( terms != null )
    kw.addAll(terms);
   else
    kw.add(pVal);
   
  }

  if( hasDescription )
   out.append("\n</description>\n");
  
  for( MSI msi : grp.getMSIs() )
  {
   if( msi.getDescription() != null )
   {
    Collection<String> terms = expander.expand(msi.getDescription().trim(), true );
    
    if( terms != null )
     kw.addAll(terms);
    else
     kw.add(msi.getDescription());
   }
   

   
   for( Publication pb : msi.getPublications() )
   {
    kw.add( pb.getAuthorList() );
    kw.add( pb.getDOI() );
    kw.add( pb.getEditor() );
    kw.add( pb.getIssue() );
    kw.add( pb.getPublisher() );
    kw.add( pb.getPubmedId() );
    kw.add( pb.getTitle() );
    kw.add( pb.getVolume() );
    kw.add( pb.getYear() );
    
    if(  pb.getStatus() != null )
     kw.add( pb.getStatus().getName() );
   }
   
   for( Contact ct : msi.getContacts() )
   {
    kw.add( ct.getAddress() );
    kw.add( ct.getAffiliation() );
    kw.add( ct.getEmail() );
    kw.add( ct.getFax() );
    kw.add( ct.getFirstName() );
    kw.add( ct.getLastName() );
   }
   
   for( DatabaseRefSource db : msi.getDatabases() )
   {
    kw.add( db.getDescription() );
    kw.add(db.getName());
   }

   for( Organization org : msi.getOrganizations() )
   {
    kw.add( org.getAddress() );
    kw.add( org.getDescription() );
    kw.add( org.getEmail() );
    kw.add( org.getFax() );
    kw.add( org.getName() );
    kw.add( org.getPhone() );
   }
   
   
  }
  
  Matcher propNameMtch = Pattern.compile("^(characteristic|comment)\\[(.+)\\]$",Pattern.CASE_INSENSITIVE).matcher("");

  for( BioSample s : grp.getSamples() )
  {
   if( ! s.isPublic() )
    continue;

   for( ExperimentalPropertyValue<? extends ExperimentalPropertyType> sprop : s.getPropertyValues() )
   {
   
     String pName = sprop.getType().getTermText().trim();
     
     propNameMtch.reset(pName);
     
     if( propNameMtch.matches() )
      pName = propNameMtch.group(2);
    
    Collection<String> terms = expander.expand(pName.toLowerCase().trim(), false );
    
    if( terms != null )
     kw.addAll(terms);
    else
     kw.add(pName);
    
    String pVal = sprop.getTermText();
    
    terms = expander.expand(pVal.toLowerCase().trim(), DESCRIPTION_PROPERTY.equalsIgnoreCase(pName));

    if( terms != null )
     kw.addAll(terms);
    else
     kw.add(pVal);
   }
  }
  
  out.append("\n<keywords>\n");

  kw.remove(null);
  
  for( String w : kw )
  {
   xmlEscaped(w, out);
   out.append(' ');
  }
  
  out.append("\n</keywords>\n</entry>\n"); 
 
  return true;
 }

 @Override
 public void exportHeader( long since, Appendable out ) throws IOException
 {
  Date d = new Date();
  
  
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
 }


 @Override
 public void shutdown()
 {
 }

}
