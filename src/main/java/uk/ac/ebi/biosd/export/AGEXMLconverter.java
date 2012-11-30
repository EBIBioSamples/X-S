package uk.ac.ebi.biosd.export;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;

public class AGEXMLconverter extends AbstractXMLexport
{
 private static String nameSpace = "http://www.ebi.ac.uk/biosamples/SampleGroupExportV1";
 
 
 @Override
 public void exportSample(BioSample smp, Appendable out) throws IOException
 {
  exportSample(smp, out, true, true, null, null);
 }

 @Override
 public void exportGroup(BioSampleGroup ao, Appendable out) throws IOException
 {
  exportGroup(ao, out, true, Samples.LIST, false );
 }

 @Override
 public void exportGroup(BioSampleGroup ao, Appendable out, boolean showNS, Samples smpSts, boolean showAttributes) throws IOException
 {
  Set<String> attrset = null;
  
  if( showAttributes )
   attrset = new HashSet<>();
  
  out.append("<SampleGroup ");
  
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
    out.append("<attribute class=\"SubmissionDate\" classDefined=\"true\" dataType=\"DATETIME\">\n<value>");
    out.append( dateTimeFmt.format(msi.getSubmissionDate() ) );
    out.append("</value>\n</attribute>\n");
   }
   
   if( msi.getReleaseDate() != null )
   {
    out.append("<attribute class=\"ReleaseDate\" classDefined=\"true\" dataType=\"DATETIME\">\n<value>");
    out.append( dateTimeFmt.format(msi.getReleaseDate() ) );
    out.append("</value>\n</attribute>\n");
   }

   if( msi.getUpdateDate() != null )
   {
    out.append("<attribute class=\"UpdateDate\" classDefined=\"true\" dataType=\"DATETIME\">\n<value>");
    out.append( dateTimeFmt.format(msi.getUpdateDate() ) );
    out.append("</value>\n</attribute>\n");
   }
   
   if( msi.getReferenceSources() != null && msi.getReferenceSources().size() > 0 )
   {
    out.append("<attribute class=\"Term Sources\" classDefined=\"true\" dataType=\"OBJECT\">\n");

    for( ReferenceSource c : msi.getReferenceSources() )
     exportReferenceSources(c, out);
    
    out.append("</attribute>\n");
   }
   
   if( msi.getOrganizations() != null && msi.getOrganizations().size() > 0 )
   {
    out.append("<attribute class=\"Organizations\" classDefined=\"true\" dataType=\"OBJECT\">\n");

    for( Organization c : msi.getOrganizations() )
     exportOrganization(c, out);    
 
    out.append("</attribute>\n");
   }

   if( msi.getContacts() != null )
   {
    out.append("<attribute class=\"Persons\" classDefined=\"true\" dataType=\"OBJECT\">\n");

    for( Contact c : msi.getContacts() )
     exportPerson(c, out); 
    out.append("</attribute>\n");
   }
   
   if( msi.getDatabases() != null )
   {
    for( DatabaseRefSource c : msi.getDatabases() )
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

  
  if( smpSts != Samples.NONE && ao.getSamples() != null )
  {
    for( BioSample smp : ao.getSamples() )
    {
     exportSample(smp, out, false, smpSts == Samples.EMBED, ao.getAcc(), attrset);
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
  
  
  out.append("</SampleGroup>\n");
 }

 @Override
 public void exportHeader(long ts,  Appendable out) throws IOException
 {
  out.append("<BioSamples xmlns=\"");
  xmlEscaped(nameSpace, out);
  
  if( ts > 0 )
   out.append("\" timestamp=\"").append( String.valueOf(ts) );
  
  out.append("\">\n");
 }


 @Override
 public void exportFooter(Appendable out) throws IOException
 {
  out.append("</BioSamples>\n");
 }
 
 
 private static void exportReferenceSources( ReferenceSource rs, Appendable out ) throws IOException
 {
  out.append("<value>\n<object id=\">");
  xmlEscaped(rs.getAcc(), out);
  out.append("\" class=\"TermSource\" classDefined=\"true\">\n");
  
  String s = null;
  
  s = rs.getName();
  if( s != null && s.length() > 0 )
   exportAttribute("Term Source Name", s, out);
  
  s = rs.getUrl();
  if( s != null && s.length() > 0 )
   exportAttribute("Term Source URI", s, out);
  
  s = rs.getVersion();
  if( s != null && s.length() > 0 )
   exportAttribute("Term Source Version", s, out);

  out.append("</object>\n</value>\n");
 }
 
 private static void exportOrganization( Organization org, Appendable out ) throws IOException
 {
  out.append("<value>\n<object id=\">");
  out.append( String.valueOf(org.getId()) );
  out.append("\" class=\"Organization\" classDefined=\"true\">\n");
  
  String s = null;
  
  s = org.getName();
  if( s != null && s.length() > 0 )
   exportAttribute("Organization Name", s, out);

  s = org.getAddress();
  if( s != null && s.length() > 0 )
   exportAttribute("Organization Address", s, out);
  
  s = org.getUrl();
  if( s != null && s.length() > 0 )
   exportAttribute("Organization URI", s, out);
  
  s = org.getEmail();
  if( s != null && s.length() > 0 )
   exportAttribute("Organization Email", s, out);

  if( org.getOrganizationRoles() != null && org.getOrganizationRoles().size() > 0  )
   exportRoles("Organization Role", org.getOrganizationRoles(), out);
   
  out.append("</object>\n</value>\n");

 }
 
 private static void exportAttribute( String cls, String val, Appendable out ) throws IOException
 {
  out.append("<attribute class=\"");
  xmlEscaped(cls, out);
  out.append("\" classDefined=\"true\" dataType=\"STRING\">\n<value>");
  xmlEscaped(val,out);
  out.append("</value>\n</attribute>\n");
 }
 
 private static void exportRoles( String cls, Collection<ContactRole> roles, Appendable out ) throws IOException
 {
  out.append("<attribute class=\"");
  xmlEscaped(cls, out);
  out.append("\" classDefined=\"true\" dataType=\"STRING\">\n");
  
  for( ContactRole cr : roles )
  {
   out.append("<value>");
   xmlEscaped(cr.getName(), out);
   out.append("</value>\n");
  }

  out.append("</attribute>\n");
 }
 
 private static void exportSample(BioSample smp,  Appendable out, boolean showNS, boolean showAnnt, String grpId, Set<String> attrset) throws IOException
 {
  out.append("<Sample ");
   
  if( showNS )
  {
   out.append( "xmlns=\"");
   xmlEscaped(nameSpace, out);
   out.append( "\" ");
  }

  out.append("id=\"");
  xmlEscaped(smp.getAcc(), out);
 
  if( grpId != null )
  {
   out.append("\" groupId=\"");
   xmlEscaped(grpId, out);
  }
 
  out.append("\">\n");
  
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
    out.append("<relation class=\"derivedFrom\" targetId=\"");
    xmlEscaped(p.getAcc(), out);
    out.append("\" targetClass=\"Sample\" />\n");
   }
  }
  
  
  out.append("</Sample>");
 }

 private static void exportPropertyValue( ExperimentalPropertyValue<? extends ExperimentalPropertyType> val,  Appendable out) throws IOException
 {
  boolean isChar = ( val instanceof BioCharacteristicValue );
  boolean isComm = ( val instanceof SampleCommentValue );
  
  out.append("<attribute classDefined=\"false\" dataType=\"STRING\" ");
  
  if( isChar )
   out.append("characteristic=\"true\" ");
  else if( isComm )
   out.append("comment=\"true\" ");

  out.append("class=\"");
  xmlEscaped(val.getType().getTermText(),out);
  out.append("\">\n");
  
  
  out.append("<value>");
  xmlEscaped(val.getTermText(),out);
  out.append("</value>\n");

  if( val.getUnit() != null )
  {
   out.append("<attribute classDefined=\"true\" dataType=\"OBJECT\" class=\"Unit\">\n");
   out.append("<value>");
   xmlEscaped(val.getUnit().getTermText(),out);
   out.append("</value>\n");  
   
   Collection<OntologyEntry> unitont = val.getUnit().getOntologyTerms();
   
   if( unitont != null )
   {
    for( OntologyEntry oe: unitont )
     exportOntologyEntry( oe, out );
   }
   

   out.append("</attribute>\n");
  }
  
  if( val.getOntologyTerms() != null )
  {
   for( OntologyEntry oe: val.getOntologyTerms() )
    exportOntologyEntry( oe, out );
  }
  
  out.append("</attribute>\n");
 }
 

 
 private static void exportOntologyEntry( OntologyEntry val,  Appendable out) throws IOException
 {
  ReferenceSource src = val.getSource();

  out.append("<attribute class=\"Term Source REF\" classDefined=\"true\" dataType=\"OBJECT\">\n<value>\n");
  out.append("<object id=\""+src.getName()+"\" class=\"Term Source\" classDefined=\"true\">\n");
  
  
  if( src != null )
  {
   if( src.getName() != null )
   {
    out.append("<attribute class=\"Term Source Name\" classDefined=\"true\" dataType=\"STRING\">\n<value>");
    xmlEscaped(src.getName(), out);
    out.append("</value>\n</attribute>\n");
   }
   
  
   if( src.getUrl() != null )
   {
    out.append("<attribute class=\"Term Source URI\" classDefined=\"true\" dataType=\"STRING\">\n<value>");
    xmlEscaped(src.getUrl(), out);
    out.append("</value>\n</attribute>\n");
   }

   if( src.getVersion() != null )
   {
    out.append("<attribute class=\"Term Source Version\" classDefined=\"true\" dataType=\"STRING\">\n<value>");
    xmlEscaped(src.getVersion(), out);
    out.append("</value>\n</attribute>\n");
   }

  }
  
  if( val.getAcc() != null )
  {
   out.append("<attribute class=\"Term Source ID\" classDefined=\"true\" dataType=\"STRING\">\n<value>");
   xmlEscaped(val.getAcc(), out);
   out.append("</value>\n</attribute>\n");
  }
  
  out.append("</object>\n</value>\n</attribute>\n");

 }
 
 
}
