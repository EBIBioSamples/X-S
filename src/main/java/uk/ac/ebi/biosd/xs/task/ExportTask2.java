package uk.ac.ebi.biosd.xs.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.email.Email;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.mtexport.ExporterMTControl;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;
import uk.ac.ebi.biosd.xs.output.OutputModule;
import uk.ac.ebi.biosd.xs.service.RequestConfig;
import uk.ac.ebi.biosd.xs.service.SchemaManager;
import uk.ac.ebi.biosd.xs.util.StringUtils;

public class ExportTask2
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

 private final Collection<OutputModule> modules;
 
 private final Lock busy = new ReentrantLock();
 
 ExporterMTControl exportControl;
 
 private static Logger log = null;
 

 
 public ExportTask2(String nm, EntityManagerFactory emf, EntityManagerFactory myEqFact, Collection<OutputModule> modules) throws TaskInitError
 {
  if( log == null )
   log = LoggerFactory.getLogger(ExportTask2.class);

  name = nm;

  this.emf = emf;
  this.myEqFact = myEqFact;

  this.modules = modules;
  
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
   
   if(limit < 0)
    limit = Integer.MAX_VALUE;
   
   log.debug("Start exporting XML files for task '"+name+"'");

  
   
   synchronized(this)
   {
    exportControl = new ExporterMTControl(emf, myEqFact, modules, threads);
   }

   boolean finishedOK = true;
   
   try
   {
    ExporterStat stat = exportControl.export(-1, limit, now, grpMul, smpMul);

    ExporterStat stat = exportControl.export(-1, limit, now, taskConfig.getGroupMultiplier(null), taskConfig.getSampleMultiplier(null) );

   
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
