package uk.ac.ebi.biosd.xs.task;

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
 public static final int                   DefaultSliceSize      = 10;
 public static final int                   DefaultThreadTTL      = 1000;

 private final String name;
 
 private final EntityManagerFactory emf;
 private final EntityManagerFactory myEqFact;

 private final Collection<OutputModule> modules;
 private final TaskConfig taskConfig;
 
 private final Lock busy = new ReentrantLock();
 
 ExporterMTControl exportControl;
 
 private static Logger log = null;
 

 
 public ExportTask(String nm, EntityManagerFactory emf, EntityManagerFactory myEqFact, Collection<OutputModule> modules, TaskConfig cnf) throws TaskInitError
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  name = nm;

  this.emf = emf;
  this.myEqFact = myEqFact;

  this.modules = modules;
  
  taskConfig=cnf;
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
      
   log.debug("Start exporting XML files for task '"+name+"'");

  
   
   synchronized(this)
   {
    exportControl = new ExporterMTControl(emf, myEqFact, modules, threads, taskConfig.getSliceSize(DefaultSliceSize), taskConfig.getThreadTTL(DefaultThreadTTL));
   }

  
   Date startTime = new Date();
   
   try
   {
    ExporterStat stat = exportControl.export(-1, limit, startTime, taskConfig.getGroupMultiplier(null), taskConfig.getSampleMultiplier(null) );

    Date endTime = new Date();

    if( Email.getDefaultInstance() != null )
     if( ! Email.getDefaultInstance().sendAnnouncement("X-S task '"+name+"' success "+StringUtils.millisToString(endTime.getTime()-startTime.getTime()),
       "Task '"+name+"' has finished successfully\n\n"+stat.createReport(startTime, endTime, threads)) )
      log.error("Can't send an info announcement by email");

   }
   catch( Throwable t )
   {
    log.error("Task '"+name+"': XML generation terminated with error: "+t.getMessage());

    if( Email.getDefaultInstance() != null )
     if( ! Email.getDefaultInstance().sendErrorAnnouncement("X-S task '"+name+"' error","Task '"+name+"': XML generation terminated with error",t) )
      log.error("Can't send an error announcement by email");
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


  public TaskConfig getTaskConfig()
  {
   return taskConfig;
  }

}
