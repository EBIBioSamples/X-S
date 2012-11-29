package uk.ac.ebi.biosd.export;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Set;

import org.h2.value.DataType;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.Product;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
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
  // TODO Auto-generated method stub

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
  
  if( oldFormat )
   exportAttributedOld( ao, out, atset );
  else
   exportAttributed( ao, out, atset );

  if( showRels && ao.getRelations() != null )
  {
   for( AgeRelation rl : ao.getRelations() )
   {
    if( rl.isInferred() )
     continue;

    String clsName = rl.getAgeElClass().getName();

    out.append("<relation class=\"");
    out.append(StringUtils.xmlEscaped(clsName));
    out.append("\" targetId=\"");
    out.append(StringUtils.xmlEscaped(rl.getTargetObjectId()));
    out.append("\" targetClass=\"" +rl.getTargetObject());
    out.append(StringUtils.xmlEscaped(rl.getTargetObject().getAgeElClass().getName()));
    out.append("\" />");
    
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
  
  out.append("</attribute>\n");
 }
 
//  <attribute class="Term Source REF" classDefined="true" dataType="OBJECT">
//  <objectValue>
//  <object id="NCBI Taxonomy" class="Term Source" classDefined="true">
//  <attribute class="Term Source URI" classDefined="true" dataType="URI">
//  <simpleValue>
//  <value>http://www.ncbi.nlm.nih.gov/taxonomy/</value>
//   </simpleValue>
//  </attribute>
//  <attribute class="Term Source Name" classDefined="true" dataType="STRING">
//  <simpleValue><value>NCBI Taxonomy</value></simpleValue></attribute>
//  </object></objectValue></attribute>
//  <attribute class="Term Source ID" classDefined="true" dataType="STRING">
//  <simpleValue><value>10090</value></simpleValue></attribute><value>Mus musculus</value></simpleValue></attribute>
 
 
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
 
 private void exportAttributedOld( Attributed ao, PrintWriter out, Set<AgeAttributeClass> atset )
 {
  for( AgeAttributeClass aac : ao.getAttributeClasses() )
  {
   if( atset != null )
    atset.add(aac);
   
   out.print("<attribute class=\"");
   out.print(StringUtils.xmlEscaped(aac.getName()));
   out.println("\" classDefined=\""+(aac.isCustom()?"false":"true")+"\" dataType=\""+aac.getDataType().name()+"\">");

   for( AgeAttribute attr : ao.getAttributesByClass(aac, false) )
   {
    out.print("<value>");
    
    exportAttributedOld( attr, out, null );

    if( aac.getDataType() != DataType.OBJECT )
     out.print(StringUtils.xmlEscaped(attr.getValue().toString()));
    else
    {
     AgeObject tgtObj = (AgeObject)attr.getValue();
     
     out.print("<object id=\""+StringUtils.xmlEscaped(tgtObj.getId())+"\" class=\"");
     out.print(StringUtils.xmlEscaped(tgtObj.getAgeElClass().getName()));
     out.println("\" classDefined=\""+(tgtObj.getAgeElClass().isCustom()?"false":"true")+"\">");
 
     exportAttributedOld( tgtObj, out, null );
    
     out.println("</object>");
    }

    out.println("</value>");
   }

   out.println("</attribute>");
  }
 }
 
}
