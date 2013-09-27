package uk.ac.ebi.biosd.xs.service.ebeye;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.export.EBeyeXMLFormatter;
import uk.ac.ebi.biosd.xs.keyword.OWLKeywordExpansion;
import uk.ac.ebi.biosd.xs.service.ExporterMT;
import uk.ac.ebi.biosd.xs.service.GroupQueryManager;
import uk.ac.ebi.biosd.xs.service.ExporterMT.PoisonedObject;
import uk.ac.ebi.biosd.xs.service.ExporterMT.TaskState;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.RangeManager;
import uk.ac.ebi.biosd.xs.util.RangeManager.Range;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class EBeyeExport
{
 private static EBeyeExport instance;
 
 private static final String samplesFileName = "samples.xml";
 private static final String groupsFileName = "groups.xml";
 
 private AbstractXMLFormatter ebeyeFmt;
 private final EntityManagerFactory emf;

 private final File outDir;
 private final File tmpDir;
 private final File auxOut;
 private final AbstractXMLFormatter auxFmt;
 private final URL efoURL;
 private final int blockSize  = 500;
 
 private final AtomicBoolean busy = new AtomicBoolean( false );
 
 private final Logger log;
 
 
 public static EBeyeExport getInstance()
 {
  return instance;
 }

 public static void setInstance(EBeyeExport instance)
 {
  EBeyeExport.instance = instance;
 }
 
 public EBeyeExport(EntityManagerFactory emf, File outDir, File tmpDir, URL efoURL, File aux, AbstractXMLFormatter auxf)
 {
  this.emf=emf;
  
  this.outDir = outDir;
  this.tmpDir = tmpDir;
  this.efoURL = efoURL;
  
  auxOut = aux;
  auxFmt = auxf;
  
  log = LoggerFactory.getLogger(EBeyeExport.class);
 }
 
 
 public boolean export( int limit, boolean genSamples, boolean genGroup, boolean pubOnly, int threads ) throws IOException
 {
  if( ! busy.compareAndSet(false, true) )
  {
   log.info("Export in progress. Skiping");
   return false;
  }
  
  try
  {

   ExecutorService tPool = Executors.newFixedThreadPool(threads);
   
   BlockingQueue<Object> reqQ = new ArrayBlockingQueue<>(100);
   RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads);

   Map<String, Counter> srcMap = new HashMap<String, Counter>();
   
   
   File smplFile = new File(tmpDir, samplesFileName);
   File grpFile = new File(tmpDir, groupsFileName);

   PrintStream smplFileOut = new PrintStream(smplFile, "UTF-8");
   PrintStream grpFileOut = new PrintStream(grpFile, "UTF-8");
   PrintStream auxFileOut = null;
   
   if( auxOut != null )
    auxFileOut = new PrintStream(auxOut, "UTF-8");
   
   AtomicBoolean stopFlag = new AtomicBoolean(false);
   
   for( int i=0; i < threads; i++ )
    tPool.submit( new EBeyeExporterTask(srcMap, rm, emf, reqQ, stopFlag) );
   
   ebeyeFmt.exportHeader( -1, null );
   
    
   if(!checkDirs())
    return false;

   if(limit < 0)
    limit = Integer.MAX_VALUE;

   log.debug("Start exporting EBeye XML files");

   cleanDir(tmpDir);

   ebeyeFmt = new EBeyeXMLFormatter(new OWLKeywordExpansion(efoURL));

   int count=0;
   
   int tnum = threads;
   
   try
   {
    while(true)
    {
     Object o;

     try
     {
      o = reqQ.take();
     }
     catch(InterruptedException e)
     {
      continue;
     }

     if(o == null)
      continue;

     String s = o.toString();

     if(s == null)
     {
      if(((PoisonedObject) o).getException() != null)
      {
       stopFlag.set(true);
       reqQ.clear();

       throw new IOException(((PoisonedObject) o).getException());
      }

      tnum--;

      if(tnum == 0)
       break;
     }

     count++;

     out.append(s);

     if(limit > 0 && count >= limit)
     {
      stopFlag.set(true);
      reqQ.clear();

      break;
     }
    }
   }
   catch (Exception e) 
   {
    stopFlag.set(true);
    reqQ.clear();

    tPool.shutdown();
   
    throw e;
   }

   tPool.shutdown();
   
   while( true )
   {
    try
    {
     if( ! tPool.awaitTermination(30, TimeUnit.SECONDS) )
      System.out.println("Can't terminate thread pool");
     
     break;
    }
    catch(InterruptedException e)
    {
    }
   }
    
   if( exportSources )
    formatter.exportSources(srcMap, out);
   
   formatter.exportFooter(out);

   ebeyeFmt.exportFooter(smplFileOut);
   ebeyeFmt.exportFooter(grpFileOut);

   smplFileOut.close();
   grpFileOut.close();

   cleanDir(outDir);

   if(!smplFile.renameTo(new File(outDir, samplesFileName)))
    log.error("Moving samples file failed. {} -> {} ", smplFile.getAbsolutePath(), new File(outDir, samplesFileName).getAbsolutePath());

   if(!grpFile.renameTo(new File(outDir, groupsFileName)))
    log.error("Moving groups file failed. {} -> {} ", grpFile.getAbsolutePath(), new File(outDir, groupsFileName).getAbsolutePath());

  }
  finally
  {
   busy.set(false);
  }
 
  return true;
 }

 private boolean checkDirs()
 {
  if( ! outDir.exists() )
  {
   if( ! outDir.mkdirs() )
   {
    log.error("Can't create output directory: {}",outDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! outDir.canWrite() )
  {
   log.error("Output directory is not writable: {}",outDir.getAbsolutePath());
   return false;
  }
  
  if( ! tmpDir.exists() )
  {
   if( ! tmpDir.mkdirs() )
   {
    log.error("Can't create temp directory: {}",tmpDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! tmpDir.canWrite() )
  {
   log.error("Temp directory is not writable: {}",tmpDir.getAbsolutePath());
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

 class EBeyeExporterTask implements Callable<TaskState>
 {
  private final RangeManager rangeMngr;
  private final EntityManagerFactory emFactory;
  private final Map<String, Counter> sourcesMap;
  private final BlockingQueue<Object> resultQueue;
  private final AtomicBoolean stopFlag;
  
  EBeyeExporterTask(Map<String, Counter> srcMap, RangeManager rMgr, EntityManagerFactory emf,  BlockingQueue<Object> resQ, AtomicBoolean stf )
  {
   sourcesMap = srcMap;
   rangeMngr = rMgr;
   
   emFactory = emf;
   resultQueue = resQ;
   
   stopFlag = stf;
   
  }

  @Override
  public TaskState call()
  {
   GroupQueryManager grpq = new GroupQueryManager(emFactory);
   
   Range r = rangeMngr.getRange();


   Set<String> msiTags = new HashSet<String>();

   StringBuilder sb = new StringBuilder();


   while(r != null)
   {
    long lastId = r.getMax();

    System.out.println("Processing range: " + r + " Thread: " + Thread.currentThread().getName());
    try
    {

     for(BioSampleGroup g : grpq.getGroups( r.getMin(), r.getMax() ))
     {
      if(stopFlag.get())
       return TaskState.INTERRUPTED;

      lastId = g.getId();
      
      sb.setLength(0);

      ebeyeFmt.exportGroup(g, sb);

      msiTags.clear();
      int nSmp = g.getSamples().size();

      for(MSI msi : g.getMSIs())
      {
       for(DatabaseRefSource db : msi.getDatabases())
       {
        String scrNm = sourcesByName ? db.getName() : db.getAcc();

        if(scrNm == null)
         continue;

        scrNm = scrNm.trim();

        if(scrNm.length() == 0)
         continue;

        if(msiTags.contains(scrNm))
         continue;

        msiTags.add(scrNm);

        synchronized(sourcesMap)
        {
         Counter c = sourcesMap.get(scrNm);

         if(c == null)
          sourcesMap.put(scrNm, new Counter(nSmp));
         else
          c.add(nSmp);
        }

       }
      }

      putIntoQueue(sb.toString());

      if(stopFlag.get())
       return TaskState.INTERRUPTED;

     }

    }
    catch(IOException e)
    {
     e.printStackTrace();

     putIntoQueue(new PoisonedObject(e));

     return TaskState.IOERROR;
    }
    finally
    {
     grpq.release();
    }

    if( lastId == r.getMax() )
     r = null;
    else
     r.setMin(lastId + 1);

    r = rangeMngr.returnAndGetRange(r);

    if(r == null)
    {
     putIntoQueue(new PoisonedObject());

     //      System.out.println("Processing finished. Thread: " + Thread.currentThread().getName());

     return TaskState.OK;
    }
   }

  
   return TaskState.OK;
  }

  public void putIntoQueue( Object o )
  {

   while(true)
   {
    try
    {
     resultQueue.put(o);
    }
    catch(InterruptedException e)
    {
     if( stopFlag.get() )
      return;
     
     continue;
    }
    
    return;
   }

  }
 }
 
 enum MessageType
 
 class Message
 {
  private 
 }


}
