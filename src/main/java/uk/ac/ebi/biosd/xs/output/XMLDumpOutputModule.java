package uk.ac.ebi.biosd.xs.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;
import uk.ac.ebi.biosd.xs.task.TaskInitError;

public class XMLDumpOutputModule implements OutputModule
{
 private final String name;
 
 private XMLFormatter formatter;
 private boolean groupedSamplesOnly;
 
 private boolean showSourcesByName;
 private boolean showSourcesByAcc;
 private boolean showNS;
 
 private final File outFile;
 private final File tmpDir=null;

 private File tmpGrpFile;
 private File tmpSmpFile;
 
 private PrintStream tmpGrpStream;
 private PrintStream tmpSmpStream;
 
 private java.util.Date startTime;
 
 private static Logger log = null;
 
 public XMLDumpOutputModule(String name, Map<String, String> cfg) throws TaskInitError
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  
  this.name = name;
  
//  this.tmpDir = tmpDir;


//  String outFileName = rc.getOutput(null);
  
  String outFileName = null;
  
  if( outFileName == null )
   throw new TaskInitError("Task '"+name+"': Output file is not defined");
  
  outFile = new File(outFileName);

  if(!outFile.getParentFile().canWrite())
  {
   log.error("Task '"+name+"': Output file directory is not writable: " + outFile);
   throw new TaskInitError("Task '"+name+"': Output file directory is not writable: " + outFile);
  }
 

//  if(!checkDirs())
//   return false;
  
 }
 
 
 
 @Override
 public XMLFormatter getFormatter()
 {
  return formatter;
 }

 @Override
 public Appendable getGroupOut()
 {
  return tmpGrpStream;
 }

 @Override
 public Appendable getSampleOut()
 {
  return tmpSmpStream;
 }

 @Override
 public boolean isGroupedSamplesOnly()
 {
  return groupedSamplesOnly;
 }

 @Override
 public void start() throws IOException
 {

  startTime = new java.util.Date();

  formatter.setNowDate(startTime);

  tmpGrpFile = new File(tmpDir, "grp_" + name.hashCode() + "_" + System.currentTimeMillis() + ".tmp");

  tmpGrpStream = new PrintStream(tmpGrpFile, "UTF-8");

  log.debug("Start exporting XML files for task '" + name + "'");

  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  formatter.exportHeader(-1, tmpGrpStream, showNS);

  String commStr = "<!-- Start time: " + simpleDateFormat.format(startTime) + " -->\n";

  tmpGrpStream.append(commStr);

  formatter.exportGroupHeader(tmpGrpStream, false, -1);

  if(formatter.isSamplesExport())
  {
   tmpSmpFile = new File(tmpDir, "smp_" + name.hashCode() + "_" + System.currentTimeMillis() + ".tmp");

   tmpSmpStream = new PrintStream(tmpSmpFile, "utf-8");
  }

 }
 
 @Override
 public void finish(ExporterStat stat) throws IOException
 {
  
  formatter.exportGroupFooter(tmpGrpStream);
   
  if(formatter.isSamplesExport())
  {
   tmpSmpStream.close();

   tmpSmpStream = null;

   formatter.exportSampleHeader(tmpGrpStream, false, stat.getUniqSampleCount());

   appendFile(tmpGrpStream, tmpSmpFile);

   formatter.exportSampleFooter(tmpGrpStream);
  }

  if(showSourcesByAcc)
   formatter.exportSources(stat.getSourcesByAccMap(), tmpGrpStream);
  else if(showSourcesByName)
   formatter.exportSources(stat.getSourcesByNameMap(), tmpGrpStream);


  formatter.exportFooter(tmpGrpStream);    
  
  Date endTime = new java.util.Date();
 
  String summary = stat.createReport(startTime, endTime, stat.getThreads());
  
  tmpGrpStream.append(summary);
  
  if(tmpSmpStream != null )
   tmpSmpStream.close();
  
  if( tmpSmpFile != null )
   tmpSmpFile.delete();
  
  if( tmpGrpStream != null )
   tmpGrpStream.close();
  

  if(outFile.exists() && !outFile.delete())
   log.error("Task '"+name+"': Can't delete file: " + outFile);

  if(!tmpGrpFile.renameTo(outFile))
   log.error("Task '"+name+"': Moving aux file failed. {} -> {} ", tmpGrpFile.getAbsolutePath(), outFile.getAbsolutePath());
  
  tmpSmpStream = null;
  tmpSmpFile = null;
  tmpGrpStream = null;
  tmpGrpFile = null;
 }
 
 @Override
 public void cancel()
 {
  if(tmpSmpStream != null )
   tmpSmpStream.close();
  
  if( tmpSmpFile != null )
   tmpSmpFile.delete();
  
  if( tmpGrpStream != null )
   tmpGrpStream.close();
  
  if( tmpGrpFile != null )
   tmpGrpFile.delete();

  tmpSmpStream = null;
  tmpSmpFile = null;
  tmpGrpStream = null;
  tmpGrpFile = null;
 }

 
 private boolean checkDirs()
 {
  File outDir = outFile.getParentFile();
  
  if( ! outDir.exists() )
  {
   if( ! outDir.mkdirs() )
   {
    log.error("Task '"+name+"': Can't create output directory: {}",outDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! outDir.canWrite() )
  {
   log.error("Task '"+name+"': Output directory is not writable: {}",outDir.getAbsolutePath());
   return false;
  }
  
  if( ! tmpDir.exists() )
  {
   if( ! tmpDir.mkdirs() )
   {
    log.error("Task '"+name+"': Can't create temp directory: {}",tmpDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! tmpDir.canWrite() )
  {
   log.error("Task '"+name+"': Temp directory is not writable: {}",tmpDir.getAbsolutePath());
   return false;
  }
  
  return true;
 }

 private void appendFile(Appendable out, File f) throws IOException
 {
  Reader rd = new InputStreamReader(new FileInputStream(f), Charset.forName("utf-8"));
  
  try
  {
   
   CharBuffer buf = CharBuffer.allocate(4096);
   
   while(rd.read(buf) != -1)
   {
    String str = new String(buf.array(), 0, buf.position());
    
    out.append(str);
    
    buf.clear();
   }
   
  }
  finally
  {
   rd.close();
  }
 }



 @Override
 public boolean isSourcesByAcc()
 {
  return showSourcesByAcc;
 }



 @Override
 public boolean isSourcesByName()
 {
  return showSourcesByName;
 }
}
