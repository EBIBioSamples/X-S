package uk.ac.ebi.biosd.xs.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.ebi.fg.biosd.model.access_control.User;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class AGE1GSXMLFormatter extends AGE1XMLFormatter
{

 
 private final Lock lock = new ReentrantLock();
 
 private File tmpFile;
 private PrintStream smpStream;
 
 private final Set<String> sampleSet = new HashSet<>();
 
 public AGE1GSXMLFormatter(boolean showNS, boolean showAttributes, boolean showAC, SamplesFormat smpfmt)
 {
  super(showNS, showAttributes, showAC, smpfmt);
 }
 
 
 @Override
 protected boolean exportSample(BioSample smp, Appendable out, boolean showNS, boolean showAnnt, boolean showGrpId, Set<String> attrset, boolean showAC) throws IOException
 {
 
  try
  {
   lock.lock();
   
   if( sampleSet.contains(smp.getAcc()) )
    return false;

   sampleSet.add(smp.getAcc());
   
   return super.exportSample(smp, out, showNS, showAnnt, showGrpId, attrset, showAC);
  }
  finally
  {
   lock.unlock();
  }
  
 }
 
 @Override
 public boolean exportSample(BioSample smp, Appendable out) throws IOException
 {
  return super.exportSample(smp, out, isShowNS(), isShowAttributes(), true, null, isShowAC());
 }

 @Override
 public boolean exportGroup(BioSampleGroup ao, Appendable out) throws IOException
 {
  return super.exportGroup(ao, out, smpStream, isShowNS(), getSamplesFormat(), isShowAttributes(), isShowAC() );
 }
 
 protected interface ACObj
 {
  Set<User> getUsers();
  boolean isPublic();
 }
 
 protected void exportAC(ACObj ao, Appendable out) throws IOException
 {
  if(ao.isPublic())
   return;
  
  out.append("public=\"false\" access=\"");

  boolean first = true;

  for(User u : ao.getUsers())
  {
   if(!first)
    out.append(',');
   else
    first = false;

   out.append(u.getName());
  }

  out.append("\" ");
 }



 @Override
 public void exportHeader(long ts, long since, Appendable out) throws IOException
 {
  super.exportHeader(ts, since, out);
  
  smpStream = new PrintStream(File.createTempFile("XSexport", ".tmp"),"utf-8");
  sampleSet.clear();
 }


 @Override
 public void exportFooter(Appendable out) throws IOException
 {
  smpStream.close();
  sampleSet.clear();
  
  Reader rd = new InputStreamReader( new FileInputStream(tmpFile), Charset.forName("utf-8"));
  
  try
  {

   CharBuffer buf = CharBuffer.allocate(4096);
   
   while( rd.read(buf) != -1 )
   {
    out.append(buf);
    
    buf.clear();
   }
   
  }
  finally
  {
   rd.close();
  }
  
  tmpFile.delete();
  
  super.exportFooter(out);
 } 
 
}
