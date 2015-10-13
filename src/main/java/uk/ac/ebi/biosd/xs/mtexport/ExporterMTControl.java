package uk.ac.ebi.biosd.xs.mtexport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.output.OutputModule;

public class ExporterMTControl
{
 static final long THREAD_TERM_WT = 30000;
 
 private final Logger log = LoggerFactory.getLogger(ExporterMTControl.class);
 
 private final EntityManagerFactory emf;
 private final EntityManagerFactory myEqFact;
 final  Collection<OutputModule> requests;
 
 private int sliceSz=10;
 private int itemsPerThreadHardLimit=4000;
 private int itemsPerThreadSoftLimit=5000;

 private final int threads;
 
 private final BlockingQueue<ControlMessage> controlMsgQueue;
 private final Lock busyLock = new ReentrantLock();
 
 private File logDir;
 
 public ExporterMTControl(EntityManagerFactory emf, EntityManagerFactory myEqFact, Collection<OutputModule> mods, int thN, int slSz, int stfTTL, int hrdThr, File logDir  )
 {
  super();
  this.emf = emf;
  this.myEqFact=myEqFact;
  this.requests = mods;
  sliceSz = slSz;
  itemsPerThreadSoftLimit = stfTTL;
  itemsPerThreadHardLimit = hrdThr;
  
  threads = thN;
  
  controlMsgQueue = new ArrayBlockingQueue<>(requests.size()*3+1);
  
  this.logDir = logDir;
 }

 
 public ExporterStat export( long since, long limit, Date now, Double grpMul, Double smpMul) throws Throwable
 {
  if(!busyLock.tryLock())
   throw new ExporterBusyException();

  try
  {

   List<Runnable> exporters = new ArrayList<>(threads);
   List<OutputTask> outputs = new ArrayList<>(requests.size() * 2);

   List<FormattingTask> tasks = new ArrayList<>(requests.size());

   controlMsgQueue.clear();

   for(OutputModule req : requests)
   {
    req.start();
    
    BlockingQueue<Object> grQueue = null;
    BlockingQueue<Object> smQueue = null;

    if(req.getGroupOut() != null)
    {
     grQueue = new ArrayBlockingQueue<>(100);
     outputs.add(new OutputTask(req.getName()+"-grp",req.getGroupOut(), grQueue, controlMsgQueue));
    }
    
    if(req.getSampleOut() != null)
    {
     smQueue = new ArrayBlockingQueue<>(100);
     outputs.add(new OutputTask(req.getName()+"-smp",req.getSampleOut(), smQueue, controlMsgQueue));
    }


    tasks.add(new FormattingTask(req.getFormatter(), req.isGroupedSamplesOnly(),
      req.isSourcesByAcc(), req.isSourcesByName(), grQueue, smQueue, limit));

   }

//   ExecutorService tPool = Executors.newFixedThreadPool(threads + outputs.size());

   Set<Thread> thrSet = new HashSet<>();
   

   AtomicBoolean stopFlag = new AtomicBoolean(false);

   ExporterStat statistics = new ExporterStat(now);
   statistics.setThreads(threads);

   for(OutputTask ot : outputs)
   {
    thrSet.add( new Thread(ot) );
    //tPool.submit(ot);
   }

   MTTaskConfig tCnf = new MTTaskConfig();
   
   tCnf.setGroupMultiplier(grpMul);
   tCnf.setSampleMultiplier(smpMul);
   tCnf.setSince(since);
   tCnf.setItemsPerThreadHardLimit(itemsPerThreadHardLimit);
   tCnf.setItemsPerThreadSoftLimit(itemsPerThreadSoftLimit);
   tCnf.setThreadLogDir(logDir);
   
   if( logDir != null )
   {
    for( File f : logDir.listFiles() )
     f.delete();
   }
   
   //RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads*2);
   //SliceManager slmnrg = new SliceManager(sliceSz);
   //SGIDSliceManager slmngr = new SGIDSliceManager(emf, sliceSz, since);
   SGIDBagManager slmngr = new SGIDBagManager(emf, sliceSz, since);

   if( logDir != null )
    slmngr.dumpSGids(logDir);
   
   for(int i = 0; i < threads; i++)
   {
//    QueryManager qm = new RangeQueryManager(emf, rm, sliceSz, tCnf.getSince());
//    MTSGExporterTask et = new SGExporterTask(myEqFact, qm, tasks, statistics, controlMsgQueue, stopFlag, tCnf);

//    QueryManager qm = new SliceQueryManager(emf, slmnrg,  tCnf.getSince());
//    MTSliceMSIExporterTask et = new MTSliceMSIExporterTask(emf, myEqFact, msism, tasks, statistics, controlMsgQueue, stopFlag, tCnf);

    IDPrefetchQueryManager idqm = new IDBagQueryManager(emf, slmngr);
    IDPrefetchExporterTask et = new IDPrefetchExporterTask(myEqFact, idqm, tasks, statistics, controlMsgQueue, stopFlag, tCnf);

    et.setLaneNo(i+1);
    
    exporters.add(et);

    thrSet.add( new Thread(et) );
    //tPool.submit(ot);

//    tPool.submit(et);
   }
   
   for( Thread t : thrSet )
    t.start();

   int tproc = threads;
   int tout = outputs.size();
   
   boolean cleanFinish = true;

   Throwable exception = null;

   boolean outputTermGoes = false;

   while(true)
   {
    ControlMessage o;

    try
    {
     o = controlMsgQueue.take();
    }
    catch(InterruptedException e)
    {
     continue;
    }

    if(o.getType() == Type.PROCESS_FINISH)
     tproc--;
    else if(o.getType() == Type.OUTPUT_FINISH)
    {
     tout--;
    }
    else if(o.getType() == Type.OUTPUT_ERROR)
    {
     log.error("Got output error. Sending termination to processing threads");

     logException(o);

     tout--;
     stopFlag.set(true);

     ((OutputTask) (o.getSubject())).getIncomingQueue().clear(); // To unlock processing tasks to see the stop flag.
     
     cleanFinish = false;
    }
    else if(o.getType() == Type.PROCESS_ERROR)
    {
     log.error("Got processing error. Sending termination to other processing threads");

     logException(o);

     tproc--;
     stopFlag.set(true);
     
     cleanFinish = false;
     exception = o.getException();
    }
    else if(o.getType() == Type.TERMINATE)
    {
     log.error("User terminate request. Sending termination to other processing threads");

     stopFlag.set(true);
     
     cleanFinish = false;
    }
    else if(o.getType() == Type.PROCESS_TTL )
    {
     MTExportTask task = (MTExportTask)o.getSubject();
     
     Thread oldThrd = task.getProcessingThread();
     
     try
     {
      oldThrd.join(30000);
     }
     catch( Exception e )
     {}
     
     if( oldThrd.isAlive() )
      log.error("Can't join terminated thread ({}). Possible thread leak",oldThrd.getName());
     
     thrSet.remove( task.getProcessingThread() );
     
     Thread t = new Thread( task );
     thrSet.add(t);
     t.start();
    }
    
    if( exception == null )
     exception = o.getException();


    if(tproc == 0 && !outputTermGoes)
    {
     log.debug("All processing thread finished. Initiating outputters shutdown");

     outputTermGoes = true;

     if( exception == null )
      exception = o.getException();

     PoisonedObject po = new PoisonedObject();

     for(FormattingTask ft : tasks)
     {
      if( ft.getGroupQueue() != null )
      {
       ft.getGroupQueue().clear();
       putIntoQueue(ft.getGroupQueue(), po);
      }
      
      if(ft.getSampleQueue() != null)
      {
       ft.getSampleQueue().clear();
       putIntoQueue(ft.getSampleQueue(), po);
      }
     }

    }

    if(tout == 0)
     break;

   }

   termLoop: for( Thread t : thrSet )
   {
    long sttm = System.currentTimeMillis();
    
    while(true)
    {
     try
     {
      t.join(THREAD_TERM_WT);
      
      break;
     }
     catch(InterruptedException e)
     {
      if( System.currentTimeMillis() - sttm >= THREAD_TERM_WT )
      {
       log.error("Can't terminate thread pool. Thread: "+t.getName());
//       System.out.println("Can't terminate thread pool. Thread: "+t.getName());

       cleanFinish = false;
       break termLoop;
      }
     }
     
     
    }   
   }

   /*
   tPool.shutdown();

   while(true)
   {
    try
    {
     if(!tPool.awaitTermination(30, TimeUnit.SECONDS))
      System.out.println("Can't terminate thread pool");

     break;
    }
    catch(InterruptedException e)
    {
    }
   }
   */
   
   if( cleanFinish )
   {
    for( OutputModule omod : requests )
     omod.finish(statistics);
   }
   else
   {
    for( OutputModule omod : requests )
     omod.cancel();
   }
   
   System.gc();

   if(exception != null)
    throw exception;

   return statistics;

  }
  finally
  {
   busyLock.unlock();
  }
 }

 
 static class PoisonedObject
 {
  IOException expt;
  
  PoisonedObject()
  {}

  PoisonedObject( IOException e )
  {
   expt = e;
  }
  
  IOException getException()
  {
   return expt;
  }
  
  @Override
  public String toString()
  {
   return null;
  }
 }

 void  putIntoQueue( BlockingQueue<Object> queue, Object o )
 {

  while(true)
  {
   try
   {
    queue.put(o);
    return;
   }
   catch(InterruptedException e)
   {
   }
  }

 }
 
 private void logException( ControlMessage o )
 {
  if(o.getException() == null)
   return;

  log.error("Exception class: " + o.getException().getClass().getName());

  if(o.getException().getMessage() != null && o.getException().getMessage().length() > 0)
   log.error("Exception message: " + o.getException().getMessage());

  log.error(o.getException().getStackTrace()[0].toString());

  o.getException().printStackTrace();
 }


 public boolean interrupt()
 {
  if( busyLock.tryLock() )
  {
   busyLock.unlock();
   return false;
  }
  
  while( true )
  {
   
   try
   {
    controlMsgQueue.put(new ControlMessage(Type.TERMINATE, null, new TerminationException()));
   }
   catch(InterruptedException e)
   {
   }
   
   break;
  } 
  
  
  busyLock.lock();
  busyLock.unlock();
  
  log.info("MT exporter has been interrupted");
  
  return true;
 }
 
}
