package uk.ac.ebi.biosd.xs.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.ebi.biosd.xs.log.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class AGE2GSXMLFormatter extends AGE2XMLFormatter
{

 
 private final Lock lock = new ReentrantLock();
 
 private File tmpFile;
 private PrintStream smpStream;
 private final StringBuilder sbcache[] = new StringBuilder[8];
 private int sbcacheptr=0;
 
 private final Set<String> sampleSet =  new HashSet<String>() ;
 
 public AGE2GSXMLFormatter( boolean showAttributes, boolean showAC, SamplesFormat smpfmt)
 {
  super(showAttributes, showAC, smpfmt);
 }
 
 @Override
 public boolean isSamplesExport()
 {
  return true;
 }
 
 @Override
 protected void exportSamples(BioSampleGroup ao, Appendable mainout, Appendable auxout, SamplesFormat smpSts, Set<String> attrset) throws IOException
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
   assert LoggerFactory.getLogger().checkpoint("Processing sample: " + smp.getAcc() + " " + (scnt++) + " of " + smpls.size(), "sblock");

   mainout.append("<Id>");
   xmlEscaped(smp.getAcc(), mainout);
   mainout.append("</Id>\n");

   
   if( smpSts != SamplesFormat.NONE )
    exportSample(smp, auxout, auxout, false, smpSts == SamplesFormat.EMBED, false, attrset, isShowAC());
  }

  mainout.append("</SampleIds>\n");

  assert LoggerFactory.getLogger().exit("End procesing sample block", "sblock");
 }
 
 @Override
 protected boolean exportSample(BioSample smp, Appendable mainout, Appendable auxout, boolean showNS, boolean showAnnt, boolean showGrpId, Set<String> attrset, boolean showAC) throws IOException
 {
  assert LoggerFactory.getLogger().entry("Start exporting sample: "+smp.getAcc(), "sample");

  StringBuilder sb = null;
  
  synchronized(sampleSet)
  {
   if( ! sampleSet.add(smp.getAcc()) )
   {
    incSampleCounter();
    return false;
   }
   
   incUniqSampleCounter();
   
   if( sbcacheptr == 0 )
    sb = new StringBuilder(4000);
   else
   {
    sb = sbcache[--sbcacheptr];
    sb.setLength(0);
   }
  }
  

  
  boolean res = super.exportSample(smp, sb, auxout, showNS, true, true, attrset, showAC);
  
  try
  {
   lock.lock();

   mainout.append(sb);
   
  }
  finally
  {
   lock.unlock();

   assert LoggerFactory.getLogger().exit("End exporting sample:"+smp.getAcc(), "sample");
  }
  
  synchronized(sampleSet)
  {
   if( sbcacheptr < sbcache.length )
    sbcache[sbcacheptr++]=sb;
  }
  
  return res;
  

 }
 
 @Override
 public boolean exportSample(BioSample smp, Appendable out, boolean showNS) throws IOException
 {
  return super.exportSample(smp, out, out, showNS, isShowAttributes(), true, null, isShowAC());
 }

 @Override
 public boolean exportGroup(BioSampleGroup ao, Appendable out, boolean showNS) throws IOException
 {
  assert LoggerFactory.getLogger().entry("Start exporting group: "+ao.getAcc(), "group");

  boolean res = super.exportGroup(ao, out, smpStream, showNS, getSamplesFormat(), isShowAttributes(), isShowAC() );
  
  assert LoggerFactory.getLogger().exit("End exporting group: "+ao.getAcc(), "group");

  return res;

 }
 
 
 @Override
 public void exportHeader( long since, Appendable out, boolean showNS) throws IOException
 {
  super.exportHeader(since, out, showNS);
  
  out.append("<SampleGroups>\n");
  
  tmpFile = File.createTempFile("XSexport", ".tmp");
  
  System.out.println("Tmp file: "+tmpFile.getAbsolutePath());
  
  smpStream = new PrintStream(tmpFile,"utf-8");
  sampleSet.clear();
 }


 @Override
 public void exportFooter(Appendable out) throws IOException
 {
  smpStream.close();
  sampleSet.clear();
  
  smpStream = null;
  
  out.append("</SampleGroups>\n<Samples>\n");
 
  
  Reader rd = new InputStreamReader( new FileInputStream(tmpFile), Charset.forName("utf-8"));
  
  try
  {

   CharBuffer buf = CharBuffer.allocate(4096);
   
   while( rd.read(buf) != -1 )
   {
    String str = new String(buf.array(),0,buf.position());
    
    out.append(str);
    
    buf.clear();
   }
   
  }
  finally
  {
   rd.close();
  }
  
  tmpFile.delete();
  
  out.append("</Samples>\n");

  super.exportFooter(out);
 } 
 
 @Override
 public void shutdown()
 {
  if( smpStream != null )
  {
   smpStream.close();
   tmpFile.delete();
  }
 }
 
}
