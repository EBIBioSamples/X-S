package uk.ac.ebi.biosd.export;

import java.io.IOException;
import java.util.Iterator;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
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
 public enum Samples
 {
  NONE,
  LIST,
  EMBED
 }
 
 public static final String nameSpace = "http://www.ebi.ac.uk/biosamples/BioSDExportV1";

 
 private static void exportOntologyEntry( OntologyEntry val,  Appendable out) throws IOException
 {
  out.append("<TermSourceREF>\n");
  
  if( val.getAcc() != null )
  {
   out.append("<TermSourceID>");
   xmlEscaped(val.getAcc(), out);
   out.append("</TermSourceID>\n");
  }
  
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
  
  out.append("</TermSourceREF>\n");

 }
 
 public static void exportSample(BioSample smp,  Appendable out) throws IOException
 {
  exportSample(smp, out, true, true);
 }

 private static void exportSample(BioSample smp,  Appendable out, boolean showNS, boolean showAnnt) throws IOException
 {
  out.append("<BioSample ");
  
  if( showNS )
   out.append( "xmlns=\""+nameSpace+"\" ");

  out.append("id=\"");
  xmlEscaped(smp.getAcc(), out);
 
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
    exportPropertyValue(pval,out);
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
   xmlEscaped(val.getUnit().getTermText(),out);
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
  exportGroup(ao, out, true, Samples.LIST );
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
  
  s = cnt.getMidInitials();
  if( s != null && s.length() > 0 )
  {
   out.append("<MidInitials>");
   xmlEscaped(s, out);
   out.append("</MidInitials>\n");
  }
  
  s = cnt.getLastName();
  if( s != null && s.length() > 0 )
  {
   out.append("<LastName>");
   xmlEscaped(s, out);
   out.append("</LastName>\n");
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
  
  out.append("</Person>");

 }
 
 private static void exportDatabase( DatabaseRefSource cnt, Appendable out ) throws IOException
 {
  out.append("<Database>\n");
  
  String s = null;
  
  s = cnt.getName();
  if( s != null && s.length() > 0 )
  {
   out.append("<Name>");
   xmlEscaped(s, out);
   out.append("</Name>\n");
  }
  
  s = cnt.getUrl();
  if( s != null && s.length() > 0 )
  {
   out.append("<URL>");
   xmlEscaped(s, out);
   out.append("</URL>\n");
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

  
  s = org.getUrl();
  if( s != null && s.length() > 0 )
  {
   out.append("<URI>");
   xmlEscaped(s, out);
   out.append("</URI>\n");
  }
  
  s = org.getAddress();
  if( s != null && s.length() > 0 )
  {
   out.append("<Address>");
   xmlEscaped(s, out);
   out.append("</Address>\n");
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
 
 public static void exportGroup( BioSampleGroup ao, Appendable out, boolean showNS, Samples smpSts ) throws IOException
 {
  out.append("<BioSampleGroup ");
  
  if( showNS )
   out.append("xmlns=\""+nameSpace+"\" ");

  out.append("id=\"");
  xmlEscaped(ao.getAcc(), out);
  out.append("\">\n");

  exportAnnotations(ao, out);
  
  if( ao.getPropertyValues() != null )
  {
   for( ExperimentalPropertyValue<ExperimentalPropertyType> pval : ao.getPropertyValues() )
    exportPropertyValue(pval,out);
  }
  
  MSI msi = null;
  
  if( ao.getMSIs() != null )
  {
   Iterator<MSI> it = ao.getMSIs().iterator();
   
   if( it.hasNext() )
    msi = it.next();
  }
  
  if( msi != null )
  {
   
   if( msi.getContacts() != null )
   {
    for( Contact c : msi.getContacts() )
     exportPerson(c, out);
   }
   
   if( msi.getDatabases() != null )
   {
    for( DatabaseRefSource c : msi.getDatabases() )
     exportDatabase(c, out);
   }
   
   if( msi.getOrganizations() != null )
   {
    for( Organization c : msi.getOrganizations() )
     exportOrganization(c, out);
   }

   if( msi.getPublications() != null )
   {
    for( Publication c : msi.getPublications() )
     exportPublication(c, out);
   }
 
   if( msi.getReferenceSources() != null )
   {
    for( ReferenceSource c : msi.getReferenceSources() )
     exportReferenceSources(c, out);
   }

   msi.getReferenceSources();
  }
  
  if( smpSts != Samples.NONE && ao.getSamples() != null )
  {
    for( BioSample smp : ao.getSamples() )
    {
     exportSample(smp, out, false, smpSts == Samples.EMBED);
    }
   
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

}
