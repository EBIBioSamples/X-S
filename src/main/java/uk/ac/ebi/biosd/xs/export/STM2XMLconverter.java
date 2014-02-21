package uk.ac.ebi.biosd.xs.export;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.Product;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.ContactRole;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.toplevel.Annotation;
import uk.ac.ebi.fg.core_model.toplevel.DefaultAccessibleAnnotatable;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;



public class STM2XMLconverter
{
 
// public static final String updateDate = "Submission Update Date";
// public static final String releaseDate = "Submission Release Date";
// public static final String refLayer = "Reference Layer";
 

 public static final String nameSpace = "http://www.ebi.ac.uk/biosamples/BioSDExportV1";

 private static DateToXsdDatetimeFormatter dateTimeFmt = new DateToXsdDatetimeFormatter( TimeZone.getTimeZone("GMT") );
 
 private static void exportOntologyEntry( OntologyEntry val,  Appendable out) throws IOException
 {
  out.append("<TermSourceREF>\n");
  
  
  ReferenceSource src = val.getSource();
  if( src != null )
  {
   if( src.getName() != null )
   {
    out.append("<Name>");
    xmlEscaped(src.getName(), out);
    out.append("</Name>\n");
   }
   
  
   if( src.getUrl() != null )
   {
    out.append("<URI>");
    xmlEscaped(src.getUrl(), out);
    out.append("</URI>\n");
   }

   if( src.getVersion() != null )
   {
    out.append("<Version>");
    xmlEscaped(src.getVersion(), out);
    out.append("</Version>\n");
   }

  }
  
  if( val.getAcc() != null )
  {
   out.append("<TermSourceID>");
   xmlEscaped(val.getAcc(), out);
   out.append("</TermSourceID>\n");
  }
  
  out.append("</TermSourceREF>\n");

 }
 
 public static void exportSample(BioSample smp,  Appendable out) throws IOException
 {
  exportSample(smp, out, true, true, null, null);
 }
// <xs:element name="Annotation" type="tns:annotationType" minOccurs="0" maxOccurs="unbounded" />
// <xs:element name="Property" type="tns:propertyType" minOccurs="0" maxOccurs="unbounded" />
// <xs:element name="derivedFrom" type="tns:stringValueType" minOccurs="0" maxOccurs="unbounded" />
// <xs:element name="GroupRef" type="tns:stringValueType" minOccurs="1" maxOccurs="unbounded" />

 private static void exportSample(BioSample smp,  Appendable out, boolean showNS, boolean showAnnt, String grpId, Set<String> attrset) throws IOException
 {
  out.append("<BioSample ");
  
  if( showNS )
   out.append( "xmlns=\""+nameSpace+"\" ");

  out.append("id=\"");
  xmlEscaped(smp.getAcc(), out);
 
  if( grpId != null )
  {
   out.append("\" groupId=\"");
   xmlEscaped(grpId, out);
  }
  
  if( ! showAnnt )
  {
   out.append("\"/>\n");
   return;
  }
 
  out.append("\">\n");
 
  exportAnnotations(smp, out);
  
  if( smp.getPropertyValues() != null )
  {
   for( ExperimentalPropertyValue<ExperimentalPropertyType> pval : smp.getPropertyValues() )
   {
    exportPropertyValue(pval,out);
    
    if( attrset != null )
     attrset.add(pval.getType().getTermText());
   }
  }
  
  if( smp.getAllDerivedFrom() != null )
  {
   for( Product<?> p : smp.getAllDerivedFrom() )
   {
    out.append("<derivedFrom>");
    xmlEscaped(p.getAcc(), out);
    out.append("</derivedFrom>\n");
   }
   
   
  }
  
  Set<BioSampleGroup> grps = smp.getGroups();
  
  if( grps != null )
  {
   out.append("\n");
   for( BioSampleGroup grp : grps )
   {
    out.append("<GroupRef>");
    xmlEscaped(grp.getAcc(), out);
    out.append("</GroupRef>\n");
   }
  }
  
  if( smp.getGroups() != null)
  
  out.append("</BioSample>\n");

 }
 
 private static void exportAnnotations( DefaultAccessibleAnnotatable obj, Appendable out ) throws IOException
 {
  if(obj.getAnnotations() == null)
   return;
  
  for(Annotation annt : obj.getAnnotations())
  {
   out.append("<Annotation type=\"");
   xmlEscaped(annt.getType().getName(), out);
   out.append("\">");
   xmlEscaped(annt.getText(), out);
   out.append("\n</Annotation>\n");
  }
 }
 
 private static void exportPropertyValue( ExperimentalPropertyValue<? extends ExperimentalPropertyType> val,  Appendable out) throws IOException
 {
  boolean isChar = ( val instanceof BioCharacteristicValue );
  boolean isComm = ( val instanceof SampleCommentValue );
  
  out.append("<Property ");
  
  if( isChar )
   out.append("characteristic=\"true\" ");
  else if( isComm )
   out.append("comment=\"true\" ");

  out.append("type=\"");
  xmlEscaped(val.getType().getTermText(),out);
  out.append("\">\n");
  out.append("<Value>");
  xmlEscaped(val.getTermText(),out);
  out.append("</Value>\n");

  if( val.getUnit() != null )
  {
   out.append("<Unit>");
   out.append("<Value>");
   xmlEscaped(val.getUnit().getTermText(),out);
   out.append("</Value>\n");  
   
   Collection<OntologyEntry> unitont = val.getUnit().getOntologyTerms();
   
   if( unitont != null )
   {
    for( OntologyEntry oe: unitont )
     exportOntologyEntry( oe, out );
   }
   

   out.append("</Unit>\n");
  }
  
  if( val.getOntologyTerms() != null )
  {
   for( OntologyEntry oe: val.getOntologyTerms() )
    exportOntologyEntry( oe, out );
  }
  
  out.append("</Property>\n");
 }
 
 public static void exportGroup( BioSampleGroup ao, Appendable out ) throws IOException
 {
  exportGroup(ao, out, true, SamplesFormat.LIST, false );
 }
 
 private static void exportPerson( Contact cnt, Appendable out ) throws IOException
 {
  out.append("<Person>\n");
  
  String s = null;
  
  s = cnt.getLastName();
  if( s != null && s.length() > 0 )
  {
   out.append("<FirstName>");
   xmlEscaped(s, out);
   out.append("</FirstName>\n");
  }
  
  s = cnt.getLastName();
  if( s != null && s.length() > 0 )
  {
   out.append("<LastName>");
   xmlEscaped(s, out);
   out.append("</LastName>\n");
  }
  
  s = cnt.getMidInitials();
  if( s != null && s.length() > 0 )
  {
   out.append("<Initials>");
   xmlEscaped(s, out);
   out.append("</Initials>\n");
  }
 
  s = cnt.getEmail();
  if( s != null && s.length() > 0 )
  {
   out.append("<Email>");
   xmlEscaped(s, out);
   out.append("</Email>\n");
  }
  
  if( cnt.getContactRoles() != null )
  {
   for( ContactRole cr : cnt.getContactRoles() )
   {
    out.append("<Role>");
    xmlEscaped(cr.getName(), out);
    out.append("</Role>\n");
   }
  }
  
  out.append("</Person>\n");

 }
 
 private static void exportDatabase( DatabaseRecordRef cnt, Appendable out ) throws IOException
 {
  out.append("<Database>\n");
  
  String s = null;
  
  s = cnt.getDbName();
  if( s != null && s.length() > 0 )
  {
   out.append("<Name>");
   xmlEscaped(s, out);
   out.append("</Name>\n");
  }
  
  s = cnt.getUrl();
  if( s != null && s.length() > 0 )
  {
   out.append("<URI>");
   xmlEscaped(s, out);
   out.append("</URI>\n");
  }

  s = cnt.getAcc();
  if( s != null && s.length() > 0 )
  {
   out.append("<ID>");
   xmlEscaped(s, out);
   out.append("</ID>\n");
  }
  

  out.append("</Database>\n");

 }
 
 private static void exportOrganization( Organization org, Appendable out ) throws IOException
 {
  out.append("<Organization>\n");
  
  String s = null;
  
  s = org.getName();
  if( s != null && s.length() > 0 )
  {
   out.append("<Name>");
   xmlEscaped(s, out);
   out.append("</Name>\n");
  }

  s = org.getAddress();
  if( s != null && s.length() > 0 )
  {
   out.append("<Address>");
   xmlEscaped(s, out);
   out.append("</Address>\n");
  }
  
  s = org.getUrl();
  if( s != null && s.length() > 0 )
  {
   out.append("<URI>");
   xmlEscaped(s, out);
   out.append("</URI>\n");
  }
  
  s = org.getEmail();
  if( s != null && s.length() > 0 )
  {
   out.append("<Email>");
   xmlEscaped(s, out);
   out.append("</Email>\n");
  }

  if( org.getOrganizationRoles() != null )
  {
   for( ContactRole cr : org.getOrganizationRoles() )
   {
    out.append("<Role>");
    xmlEscaped(cr.getName(), out);
    out.append("</Role>\n");
   }
  }

  out.append("</Organization>\n");

 }

 private static void exportPublication( Publication pub, Appendable out ) throws IOException
 {
  out.append("<Publication>\n");
  
  String s = null;
  
  s = pub.getDOI();
  if( s != null && s.length() > 0 )
  {
   out.append("<DOI>");
   xmlEscaped(s, out);
   out.append("</DOI>\n");
  }
  
  s = pub.getPubmedId();
  if( s != null && s.length() > 0 )
  {
   out.append("<PubMedID>");
   xmlEscaped(s, out);
   out.append("</PubMedID>\n");
  }

  out.append("</Publication>");

 }

 
 private static void exportReferenceSources( ReferenceSource rs, Appendable out ) throws IOException
 {
  out.append("<TermSource>\n");
  
  String s = null;
  
  s = rs.getName();
  if( s != null && s.length() > 0 )
  {
   out.append("<Name>");
   xmlEscaped(s, out);
   out.append("</Name>\n");
  }
  
  s = rs.getUrl();
  if( s != null && s.length() > 0 )
  {
   out.append("<URI>");
   xmlEscaped(s, out);
   out.append("</URI>\n");
  }
  
  s = rs.getVersion();
  if( s != null && s.length() > 0 )
  {
   out.append("<Version>");
   xmlEscaped(s, out);
   out.append("</Version>\n");
  }

  out.append("</TermSource>\n");

 }
 
 public static void exportGroup( BioSampleGroup ao, Appendable out, boolean showNS, SamplesFormat smpSts, boolean showAttributes ) throws IOException
 {
  Set<String> attrset = null;
  
  if( showAttributes )
   attrset = new HashSet<>();
  
  out.append("<BioSampleGroup ");
  
  if( showNS )
   out.append("xmlns=\""+nameSpace+"\" ");

  out.append("id=\"");
  xmlEscaped(ao.getAcc(), out);
  out.append("\">\n");


  MSI msi = null;
  
  if( ao.getMSIs() != null )
  {
   Iterator<MSI> it = ao.getMSIs().iterator();
   
   if( it.hasNext() )
    msi = it.next();
  }


  if( msi != null )
  {
   if( msi.getSubmissionDate() != null )
   {
    out.append("<SubmissionDate>");
    out.append( dateTimeFmt.format(msi.getSubmissionDate() ) );
    out.append("</SubmissionDate>\n");
   }
   
   if( msi.getReleaseDate() != null )
   {
    out.append("<ReleaseDate>");
    out.append( dateTimeFmt.format(msi.getReleaseDate() ) );
    out.append("</ReleaseDate>\n");
   }

   if( msi.getUpdateDate() != null )
   {
    out.append("<UpdateDate>");
    out.append( dateTimeFmt.format(msi.getUpdateDate() ) );
    out.append("</UpdateDate>\n");
   }
   
   if( msi.getReferenceSources() != null )
   {
    for( ReferenceSource c : msi.getReferenceSources() )
     exportReferenceSources(c, out);
   }
   
   if( msi.getOrganizations() != null )
   {
    for( Organization c : msi.getOrganizations() )
     exportOrganization(c, out);
   }

   if( msi.getContacts() != null )
   {
    for( Contact c : msi.getContacts() )
     exportPerson(c, out);
   }
   
   if( msi.getDatabaseRecordRefs() != null )
   {
    for( DatabaseRecordRef c : msi.getDatabaseRecordRefs() )
     exportDatabase(c, out);
   }
   
   if( msi.getPublications() != null )
   {
    for( Publication c : msi.getPublications() )
     exportPublication(c, out);
   }

  }

  
  if( ao.getPropertyValues() != null )
  {
   for( ExperimentalPropertyValue<ExperimentalPropertyType> pval : ao.getPropertyValues() )
    exportPropertyValue(pval,out);
  }
  
  exportAnnotations(ao, out);

  
  if( smpSts != SamplesFormat.NONE && ao.getSamples() != null )
  {
    for( BioSample smp : ao.getSamples() )
    {
     exportSample(smp, out, false, smpSts == SamplesFormat.EMBED, ao.getAcc(), attrset);
    }
   
  }
  
  if( showAttributes )
  {
   out.append("<SampleAttributes>\n");

   for(String attNm : attrset)
   {
    out.append("<attribute>");
    xmlEscaped(attNm, out);
    out.append("</attribute>\n");
   }

   out.append("</SampleAttributes>\n");
  }
  
  
  out.append("</BioSampleGroup>\n");
 }

 
 
 
 public static class ReplacePair implements Comparable<ReplacePair>
 {
  char subject;
  String replacement;
  
  public ReplacePair()
  {}
  
  public ReplacePair(char first, String second)
  {
   this.subject = first;
   this.replacement = second;
  }
  
  public char getSubject()
  {
   return subject;
  }
  
  public void setSubject(char first)
  {
   this.subject = first;
  }
  
  public String getReplacement()
  {
   return replacement;
  }

  public void setReplacement(String second)
  {
   this.replacement = second;
  }

  @Override
  public int compareTo(ReplacePair toCmp)
  {
   return subject-toCmp.getSubject();
  }
  
  @Override
  public boolean equals( Object o )
  {
   return subject==((ReplacePair)o).getSubject();
  }

  @Override
  public int hashCode()
  {
   return subject;
  }
  
 }

 static final ReplacePair[] htmlPairs = { 
  new ReplacePair('"',"&quot;"),
  new ReplacePair('\'',"&#39;"),
  new ReplacePair('<',"&lt;"),
  new ReplacePair('>',"&gt;"),
  new ReplacePair('&',"&amp;"),
  };
 
 
 public static void xmlEscaped( String s, Appendable out ) throws IOException
 {
  int len = s.length();
  
  boolean escaping = false;
  
  for( int i=0; i < len; i++ )
  {
   char ch = s.charAt(i);
   
   if( ch < 0x20 && ch != 0x0D && ch != 0x0A && ch != 0x09 )
   {
    
    if( ! escaping )
    {
     out.append( s.substring(0, i) );
     escaping=true;
    }
    
    int rem = ch%16;
    
    out.append("&#").append( (ch > 15)?'1':'0' ).append( (char)(rem > 9?(rem-10+'A'):(rem+'0')) ).append(';');
   }
   else
   {
    boolean replaced = false;
    
    for( ReplacePair p : htmlPairs )
    {
     if( ch == p.getSubject() )
     {
      if( ! escaping )
      {
       out.append( s.substring(0, i) );
       escaping=true;
      }
      
      out.append( p.getReplacement() );
      replaced = true;
      break;
     }
    }
    
    if( ! replaced )
    {
     if( escaping )
      out.append(ch);
    }
   }
  }
  
  if( ! escaping )
   out.append(s);
 }
 
 static class DateToXsdDatetimeFormatter
 {

  private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  public DateToXsdDatetimeFormatter()
  {
  }

  public DateToXsdDatetimeFormatter(TimeZone timeZone)
  {
   simpleDateFormat.setTimeZone(timeZone);
  }

  /**
   * Parse a xml date string in the format produced by this class only. This
   * method cannot parse all valid xml date string formats - so don't try to use
   * it as part of a general xml parser
   */
  public synchronized Date parse(String xmlDateTime) throws ParseException
  {
   if(xmlDateTime.length() != 25)
   {
    throw new ParseException("Date not in expected xml datetime format", 0);
   }

   StringBuilder sb = new StringBuilder(xmlDateTime);
   sb.deleteCharAt(22);
   return simpleDateFormat.parse(sb.toString());
  }

  public synchronized String format(Date xmlDateTime) throws IllegalFormatException
  {
   String s = simpleDateFormat.format(xmlDateTime);
   
   StringBuilder sb = new StringBuilder(s);
   sb.insert(22, ':');
   return sb.toString();
  }

  public synchronized void setTimeZone(String timezone)
  {
   simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
  }
}

}
