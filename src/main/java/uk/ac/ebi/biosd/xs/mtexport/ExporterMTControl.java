package uk.ac.ebi.biosd.xs.mtexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.output.OutputModule;
import uk.ac.ebi.biosd.xs.util.SliceManager;

public class ExporterMTControl
{
 private final Logger log = LoggerFactory.getLogger(ExporterMTControl.class);
 
 private final EntityManagerFactory emf;
 private final EntityManagerFactory myEqFact;
 final  Collection<OutputModule> requests;

 private final int threads;
 
 private final BlockingQueue<ControlMessage> controlMsgQueue;
 private final Lock busyLock = new ReentrantLock();
 
 public ExporterMTControl(EntityManagerFactory emf, EntityManagerFactory myEqFact, Collection<OutputModule> mods, int thN )
 {
  super();
  this.emf = emf;
  this.myEqFact=myEqFact;
  this.requests = mods;

  threads = thN;
  
  controlMsgQueue = new ArrayBlockingQueue<>(requests.size()*3+1);
 }

 
 public ExporterStat export( long since, long limit, Date now, Double grpMul, Double smpMul) throws Throwable
 {
  if(!busyLock.tryLock())
   throw new ExporterBusyException();

  try
  {

   List<MTSliceExporterTask> exporters = new ArrayList<>(threads);
   List<OutputTask> outputs = new ArrayList<>(requests.size() * 2);

   List<FormattingTask> tasks = new ArrayList<>(requests.size());

   controlMsgQueue.clear();

   for(OutputModule req : requests)
   {
    BlockingQueue<Object> grQueue = new ArrayBlockingQueue<>(100);
    outputs.add(new OutputTask(req.getGroupOut(), grQueue, controlMsgQueue));

    BlockingQueue<Object> smQueue = null;

    if(req.getSampleOut() != null)
    {
     smQueue = new ArrayBlockingQueue<>(100);
     outputs.add(new OutputTask(req.getSampleOut(), smQueue, controlMsgQueue));
    }

    tasks.add(new FormattingTask(req.getFormatter(), req.isGroupedSamplesOnly(), req.isSourcesByAcc(), req.isSourcesByName(), grQueue, smQueue));
    
    req.start();
   }

   ExecutorService tPool = Executors.newFixedThreadPool(threads + outputs.size());

   //  RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads*2);
   SliceManager gsm = new SliceManager();
   SliceManager ssm = new SliceManager();

   AtomicBoolean stopFlag = new AtomicBoolean(false);

   ExporterStat statistics = new ExporterStat(now);
   statistics.setThreads(threads);

   for(OutputTask ot : outputs)
    tPool.submit(ot);

   AtomicLong limitCnt = null;

   if(limit > 0)
    limitCnt = new AtomicLong(limit);

   MTTaskConfig tCnf = new MTTaskConfig();
   
   tCnf.setGroupMultiplier(grpMul);
   tCnf.setSampleMultiplier(smpMul);
   tCnf.setSince(since);
   
   for(int i = 0; i < threads; i++)
   {
    MTSliceExporterTask et = new MTSliceExporterTask(emf, myEqFact, gsm, ssm, tasks, statistics, controlMsgQueue, stopFlag, limitCnt, tCnf);

    exporters.add(et);

    tPool.submit(et);
   }

   int tproc = threads;
   int tout = outputs.size();
   
   boolean cleanFinish = true;

   Throwable exception = null;

   boolean termGoes = false;

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
    }
    else if(o.getType() == Type.TERMINATE)
    {
     log.error("User terminate request. Sending termination to other processing threads");

     stopFlag.set(true);
     
     cleanFinish = false;
    }

    if(tproc == 0 && !termGoes)
    {
     log.debug("All processing thread finished. Initiating outputters shutdown");

     termGoes = true;

     if( exception == null )
      exception = o.getException();

     PoisonedObject po = new PoisonedObject();

     for(FormattingTask ft : tasks)
     {
      ft.getGroupQueue().clear();
      putIntoQueue(ft.getGroupQueue(), po);

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
  
  log.info("MT exported has been interrupted");
  
  return true;
 }
 
}
