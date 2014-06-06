package uk.ac.ebi.biosd.xs.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.email.Email;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterMTControl;
import uk.ac.ebi.biosd.xs.mtexport.FormattingRequest;
import uk.ac.ebi.biosd.xs.mtexport.MTExporterStat;
import uk.ac.ebi.biosd.xs.service.RequestConfig;
import uk.ac.ebi.biosd.xs.service.SchemaManager;
import uk.ac.ebi.biosd.xs.util.StringUtils;

public class ExportTask
{
 
 public static final String                DefaultSchema         = SchemaManager.STXML;
 public static final String                DefaultSamplesFormat  = SamplesFormat.EMBED.name();
 public static final boolean               DefaultShowNS         = false;
 public static final boolean               DefaultShowSources    = true;
 public static final boolean               DefaultSourcesByName  = false;
 public static final boolean               DefaultGroupedSamplesOnly  = false;

 private final String name;
 
 private final EntityManagerFactory emf;
 private final EntityManagerFactory myEqFact;

 private XMLFormatter formatter = null;
 private final File outFile;
 private final File tmpDir;

 private final RequestConfig taskConfig;
 
 private final Lock busy = new ReentrantLock();
 
 ExporterMTControl exportControl;
 
 private final Logger log;
 

 
 public ExportTask(String nm, EntityManagerFactory emf, EntityManagerFactory myEqFact, File tmpDir, RequestConfig rc) throws TaskInitError
 {
  log = LoggerFactory.getLogger(ExportTask.class);

  name = nm;

  this.emf = emf;
  this.myEqFact = myEqFact;

  this.tmpDir = tmpDir;

  taskConfig = rc;

  String outFileName = rc.getOutput(null);
  
  if( outFileName == null )
   throw new TaskInitError("Task '"+name+"': Output file is not defined");
  
  outFile = new File(outFileName);

  if(!outFile.getParentFile().canWrite())
  {
   log.error("Task '"+name+"': Output file directory is not writable: " + outFile);
   throw new TaskInitError("Task '"+name+"': Output file directory is not writable: " + outFile);
  }

  SamplesFormat smpfmt = null;

  try
  {
   smpfmt = SamplesFormat.valueOf(rc.getSamplesFormat(DefaultSamplesFormat));

   formatter = SchemaManager.getFormatter(rc.getSchema(DefaultSchema), rc.getShowAttributesSummary(true), rc.getShowAccessControl(true), smpfmt,
     rc.getPublicOnly(false), new Date());
  }
  catch(Exception e)
  {
   log.error("Task '"+name+"': Invalid sample format parameter: " + smpfmt);
  }

 }


 public boolean isBusy()
 {
  if( ! busy.tryLock() )
   return true;
  
  busy.unlock();
  
  return false;
 }
 
 public boolean export( long limit, int threads ) throws Throwable
 {
  if( ! busy.tryLock() )
  {
   log.info("Export in progress. Skiping");
   return false;
  }
  
  if( threads <= 0 )
   threads = Runtime.getRuntime().availableProcessors();
  
  try
  {
   if(!checkDirs())
    return false;
   
   Date now = new Date();
   
   formatter.setNowDate(now);


   PrintStream auxFileOut = null;


   File tmpAuxFile = new File(tmpDir, "tmp_"+name+"_"+System.currentTimeMillis()+".tmp");

   auxFileOut = new PrintStream(tmpAuxFile, "UTF-8");
   
   
   if(limit < 0)
    limit = Integer.MAX_VALUE;
   
   log.debug("Start exporting XML files for task '"+name+"'");

   
   SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   java.util.Date startTime = new java.util.Date();


  formatter.exportHeader(-1, auxFileOut, taskConfig.getShowNamespace(DefaultShowNS) );
   
  String commStr = "<!-- Start time: "+simpleDateFormat.format(startTime)+" -->\n";
   
  auxFileOut.append(commStr);
   
  formatter.exportGroupHeader(auxFileOut, false, -1);


  File tmpAuxSampleFile = null;
  PrintStream tmpAuxSampleOut=null;

   if( formatter.isSamplesExport())
   {
    tmpAuxSampleFile = new File(tmpDir, "tmp2_"+name+"_"+System.currentTimeMillis()+".tmp");

    tmpAuxSampleOut = new PrintStream(tmpAuxSampleFile, "utf-8");
   }

   List<FormattingRequest> frList= new ArrayList<>();
   
   frList.add( new FormattingRequest(formatter, auxFileOut, tmpAuxSampleOut) );
   
   
   synchronized(this)
   {
    exportControl = new ExporterMTControl(emf, myEqFact, frList, taskConfig.getShowSources(DefaultShowSources),
      taskConfig.getSourcesByName(DefaultSourcesByName), taskConfig.getGroupedSamplesOnly(DefaultGroupedSamplesOnly), threads);
   }

   boolean finishedOK = true;
   
   try
   {

    MTExporterStat stat = exportControl.export(-1, limit, now, taskConfig.getGroupMultiplier(null), taskConfig.getSampleMultiplier(null) );

   
    formatter.exportGroupFooter(auxFileOut);
     
    if(formatter.isSamplesExport())
    {
     tmpAuxSampleOut.close();

     tmpAuxSampleOut = null;

     formatter.exportSampleHeader(auxFileOut, false, stat.getUniqSampleCount());

     appendFile(auxFileOut, tmpAuxSampleFile);

     formatter.exportSampleFooter(auxFileOut);
    }

    if(taskConfig.getShowSources(DefaultShowSources))
     formatter.exportSources(stat.getSourcesMap(), auxFileOut);

    formatter.exportFooter(auxFileOut);    
    
    Date endTime = new java.util.Date();
   
    String summary = stat.createReport(startTime, endTime, threads);
    
    auxFileOut.append(summary);

    if( Email.getDefaultInstance() != null )
     if( ! Email.getDefaultInstance().sendAnnouncement("X-S task '"+name+"' success "+StringUtils.millisToString(endTime.getTime()-startTime.getTime()),
       "Task '"+name+"' has finished successfully\n\n"+summary) )
      log.error("Can't send an info announcement by email");

   }
   catch( Throwable t )
   {
    finishedOK = false; 
    
    log.error("Task '"+name+"': XML generation terminated with error: "+t.getMessage());

    if( Email.getDefaultInstance() != null )
     if( ! Email.getDefaultInstance().sendErrorAnnouncement("X-S task '"+name+"' error","Task '"+name+"': XML generation terminated with error",t) )
      log.error("Can't send an error announcement by email");
   }
   finally
   {
    if(tmpAuxSampleOut != null )
     tmpAuxSampleOut.close();
    
    if( tmpAuxSampleFile != null )
     tmpAuxSampleFile.delete();
    
    if( auxFileOut != null )
     auxFileOut.close();
    
   }
   
   if( finishedOK )
   {

    if(outFile.exists() && !outFile.delete())
     log.error("Task '"+name+"': Can't delete file: " + outFile);

    if(!tmpAuxFile.renameTo(outFile))
     log.error("Task '"+name+"': Moving aux file failed. {} -> {} ", tmpAuxFile.getAbsolutePath(), outFile.getAbsolutePath());

   }
   else
   {
     tmpAuxFile.delete();
   }

  }
  finally
  {
   busy.unlock();
   
   synchronized(this)
   {
    if( exportControl != null )
     exportControl.interrupt();
    
    exportControl = null;
   }
   
  }
 
  return true;
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
 
 private void cleanDir(File dir)
 {
  for( File f : dir.listFiles() )
  {
   if( f.isDirectory() )
    cleanDir(f);
    
   f.delete();
  }
  
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

  public boolean interrupt()
  {
   
   if( busy.tryLock() )
   {
    busy.unlock();
    return false;
   }
   
   synchronized(this)
   {
    if( exportControl != null )
     exportControl.interrupt();
   }
   
   busy.lock();
   busy.unlock();
   
   return true;
   
  }


  public String getName()
  {
   return name;
  }


  public RequestConfig getRequestConfig()
  {
   return taskConfig;
  }

}
