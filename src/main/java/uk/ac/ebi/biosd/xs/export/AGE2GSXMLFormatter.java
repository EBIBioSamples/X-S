package uk.ac.ebi.biosd.xs.export;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import uk.ac.ebi.biosd.xs.log.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class AGE2GSXMLFormatter extends AGE2XMLFormatter
{
 
 public AGE2GSXMLFormatter( boolean showAttributes, boolean showAC, SamplesFormat smpfmt, boolean pubOnly)
 {
  super(showAttributes, showAC, smpfmt, pubOnly);
 }
 
 @Override
 public boolean isSamplesExport()
 {
  return true;
 }
 
 @Override
 protected void exportSamples(BioSampleGroup ao, Appendable mainout, SamplesFormat smpSts, Set<String> attrset) throws IOException
 {
  if(ao.getSamples() == null)
   return;

  assert LoggerFactory.getLogger().entry("Start procesing sample block", "sblock");

  Collection<BioSample> smpls = ao.getSamples();

  assert LoggerFactory.getLogger().checkpoint("Got samples: " + smpls.size(), "sblock");

  int scnt = 1;

  mainout.append("<SampleIds>\n");

  for(BioSample smp : smpls)
  {
   if( isPublicOnly() && ! smp.isPublic() )
    continue;

   assert LoggerFactory.getLogger().checkpoint("Processing sample: " + smp.getAcc() + " " + (scnt++) + " of " + smpls.size(), "sblock");
   
   mainout.append("<Id>");
   xmlEscaped(smp.getAcc(), mainout);
   mainout.append("</Id>\n");

   
//   if( smpSts != SamplesFormat.NONE )
//    exportSample(smp, mainout, false, smpSts == SamplesFormat.EMBED, false, attrset, isShowAC());
  }

  mainout.append("</SampleIds>\n");

  assert LoggerFactory.getLogger().exit("End procesing sample block", "sblock");
 }
 
 
 @Override
 public boolean exportSample(BioSample smp, Appendable out, boolean showNS) throws IOException
 {
  assert LoggerFactory.getLogger().entry("Start exporting sample: "+smp.getAcc(), "sample");
  
  boolean res =  super.exportSample(smp, out, showNS, isShowAttributes(), true, null, isShowAC());

  assert LoggerFactory.getLogger().exit("End exporting sample: "+smp.getAcc(), "sample");
  
  return res;
 }

 @Override
 public boolean exportGroup(BioSampleGroup ao, Appendable out, boolean showNS) throws IOException
 {
  assert LoggerFactory.getLogger().entry("Start exporting group: "+ao.getAcc(), "group");

  boolean res = super.exportGroup(ao, out, showNS, getSamplesFormat(), isShowAttributes(), isShowAC() );
  
  assert LoggerFactory.getLogger().exit("End exporting group: "+ao.getAcc(), "group");

  return res;

 }
 
 @Override
 public void exportGroupHeader(Appendable out, boolean showNS) throws IOException
 {

  if( showNS )
   out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
  
  out.append("<SampleGroups");
  
  if( showNS )
  {
   out.append(" xmlns=\"");
   xmlEscaped(getNameSpace(), out);
   out.append("\"");

   Date startTime = new java.util.Date();
   out.append(" timestamp=\"").append( String.valueOf(startTime.getTime()) ).append("\"");
   
   nsShown = true;
  }
  
  out.append(">\n");
  
 }
 
 @Override
 public void exportGroupFooter(Appendable out) throws IOException
 {
  out.append("</SampleGroups>\n");
 }
 
 @Override
 public void exportSampleHeader(Appendable out, boolean showNS) throws IOException
 {

  if( showNS )
   out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
  
  out.append("<Samples");
  
  if( showNS )
  {
   out.append(" xmlns=\"");
   xmlEscaped(getNameSpace(), out);
   out.append("\"");

   Date startTime = new java.util.Date();
   out.append(" timestamp=\"").append( String.valueOf(startTime.getTime()) ).append("\"");
   
   nsShown = true;
  }
  
  out.append(">\n");
  
 }
 
 @Override
 public void exportSampleFooter(Appendable out) throws IOException
 {
  out.append("</Samples>\n");
 }
 

}
