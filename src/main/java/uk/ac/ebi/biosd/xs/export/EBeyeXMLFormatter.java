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
 private final Map<String, String> ebeyeSrcMap;
 
 public EBeyeXMLFormatter(OWLKeywordExpansion exp, Map<String, String> ebeyeSrcMap, boolean pubOnly, Date now )
 {
  super(false, false, null, pubOnly, now);
  
  expander=exp;
  this.ebeyeSrcMap = ebeyeSrcMap;
 }

 static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

 @Override
 public boolean exportSample(BioSample smp,  Appendable out, boolean showNS) throws IOException
 {
  if( isPublicOnly() && ! isSamplePublic(smp) )
   return false;
  
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

    
//    if( isChar )
//     xmlEscaped("Characteristic["+pName+"]",out);
//    else if( isComm )
//     xmlEscaped("Comment["+pName+"]",out);
//    else
     xmlEscaped(pName,out);

    xmlEscaped(": "+pVal+"\n", out);
   }
  }
  
  out.append("</description>");
  
  StringBuilder sb = new StringBuilder();
  
  for( DatabaseRefSource db : smp.getDatabases() )
  {
   kw.add( db.getDescription() );
   kw.add(db.getName());
   
   if( ebeyeSrcMap != null && db.getName() != null && db.getAcc() != null )
   {
    String dbTag = ebeyeSrcMap.get(db.getName());
    
    if( dbTag != null )
    {
     sb.append("\n<ref dbkey=\"");
     xmlEscaped(db.getAcc(), sb);
     sb.append("\" dbname=\""); 
     xmlEscaped(dbTag, sb);
     sb.append("\" />\n");
    }
   }
   
  }
  
  if( sb.length() != 0 )
  {
   out.append("\n<cross_references>");
   out.append(sb.toString());
   out.append("</cross_references>");
  }
  
  
  out.append("\n<additional_fields>\n");

  out.append("<field name=\"id\">");
  xmlEscaped(smp.getAcc(), out);
  out.append("</field>\n");
  
  out.append("<field name=\"searchwords\">\n");
  
  kw = cleanKeywords(kw);
  
  for( String w : kw )
  {
   xmlEscaped(w, out);
   out.append(' ');
  }
  
  xmlEscaped(smp.getAcc(), out);
  
  out.append("\n</field>\n</additional_fields>\n</entry>\n"); 

 
  return true;
 }
 
 
 private Set<String> cleanKeywords( Set<String> kw )
 {
  Set<String> nkw = new HashSet<>();

  kw.remove(null);

  for(String w : kw)
  {
   int l = w.length();

   int begin = -1;

   int i = 0;

   while(i < l)
   {
    char ch = w.charAt(i);
    if(Character.isLetterOrDigit(ch) || ch > 255)
    {
     if(begin == -1)
      begin = i;
    }
    else
    {
     if(begin != -1)
     {
      nkw.add(w.substring(begin, i));
      begin = -1;
     }
    }
    
    i++;
   }
   
   if( begin != -1 )
    nkw.add(w.substring(begin));
  }

  return nkw;
  
 }

 @Override
 public boolean exportGroup(BioSampleGroup grp, Appendable out, boolean showNS) throws IOException
 {
  if( isPublicOnly() && ! isGroupPublic(grp) )
   return false;

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
    
    if( ! hasDescription )
    {
     out.append("\n<description>\n");
     xmlEscaped(msi.getDescription(), out);
     out.append("\n</description>");
     
     hasDescription = true;
    }
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
   
   StringBuilder sb = new StringBuilder();
   
   for( DatabaseRefSource db : msi.getDatabases() )
   {
    kw.add( db.getDescription() );
    kw.add(db.getName());
    
    if( ebeyeSrcMap != null && db.getName() != null && db.getAcc() != null )
    {
     String dbTag = ebeyeSrcMap.get(db.getName());
     
     if( dbTag != null )
     {
      sb.append("\n<ref dbkey=\"");
      xmlEscaped(db.getAcc(), sb);
      sb.append("\" dbname=\""); 
      xmlEscaped(dbTag, sb);
      sb.append("\" />\n");
     }
    }
    
   }
   
   if( sb.length() != 0 )
   {
    out.append("\n<cross_references>");
    out.append(sb.toString());
    out.append("</cross_references>");
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
   if( ! isSamplePublic(s) )
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
  
  out.append("\n<additional_fields>\n");

  out.append("<field name=\"id\">");
  xmlEscaped(grp.getAcc(), out);
  out.append("</field>\n");
  
  out.append("<field name=\"searchwords\">\n");

  kw = cleanKeywords(kw);
  
  for( String w : kw )
  {
   xmlEscaped(w, out);
   out.append(' ');
  }
  
  xmlEscaped(grp.getAcc(), out);
  
  out.append("\n</field>\n</additional_fields>\n</entry>\n"); 
 
  return true;
 }

 @Override
 public void exportSampleHeader(Appendable out, boolean showNS, int n) throws IOException
 {
  header(-1, out, showNS, n);
 }



 @Override
 public void exportSampleFooter(Appendable out) throws IOException
 {
  exportFooter(out);
 }
 
 private void header( long since, Appendable out, boolean showNS, int n ) throws IOException
 {
  Date d = new Date();
  
  
  out.append("<database>\n<name>BioSamples database</name>\n<release_date>");
  out.append(df.format(d));
  out.append("</release_date>\n");
  
  if( n >= 0 )
   out.append("<entry_count>"+n+"</entry_count>\n");
  
  out.append("<entries>\n");

 }
 
 @Override
 public void exportHeader( long since, Appendable out, boolean showNS ) throws IOException
 {
  header(since, out, showNS, -1);
 }

 @Override
 public void exportFooter(Appendable out) throws IOException
 {
  out.append("</entries>\n</database>");
 }

 @Override
 public void exportSources(Map<String, Counter> srcMap, Appendable out) throws IOException
 {
 }


 @Override
 public void shutdown()
 {
 }


 @Override
 public boolean isSamplesExport()
 {
  return false;
 }

 @Override
 public void exportGroupHeader(Appendable out, boolean showNS, int n) throws IOException
 {
  header(-1, out, showNS, n);
 }

 
 @Override
 public void exportGroupFooter(Appendable out) throws IOException
 {
  exportFooter(out);
 }



}
