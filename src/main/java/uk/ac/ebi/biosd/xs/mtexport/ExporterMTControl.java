package uk.ac.ebi.biosd.xs.mtexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.util.SliceManager;

public class ExporterMTControl
{
 private final EntityManagerFactory emf;
 final  List<FormattingRequest> requests;
 private final boolean exportSources;
 final boolean sourcesByName;
 private final int threads;
 
 public ExporterMTControl(EntityManagerFactory emf, List<FormattingRequest> ftasks, boolean exportSources, boolean sourcesByName, int thN)
 {
  super();
  this.emf = emf;
  this.requests = ftasks;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  threads = thN;
 }

 
 public MTExporterStat export( long since, long limit) throws Throwable
 {
  List<MTSliceExporterTask> exporters = new ArrayList<>( threads );
  List<OutputTask> outputs = new ArrayList<>( requests.size() * 2);
  
  List<FormattingTask> tasks = new ArrayList<>( requests.size() );
  
  BlockingQueue<ControlMessage> msgQ = new ArrayBlockingQueue<>(requests.size()*3+1);

  
  for( FormattingRequest req : requests )
  {
   BlockingQueue<Object> grQueue = new ArrayBlockingQueue<>(100);
   outputs.add( new OutputTask(req.getGroupOut(), grQueue, msgQ) );
   
   BlockingQueue<Object> smQueue=null;
   
   if( req.getSampleOut() != null )
   {
    smQueue = new ArrayBlockingQueue<>(100);
    outputs.add( new OutputTask(req.getSampleOut(), smQueue, msgQ) );
   }
   
   tasks.add( new FormattingTask(req.getFormatter(), grQueue, smQueue) );
  }
  
  ExecutorService tPool = Executors.newFixedThreadPool(threads+outputs.size());
  
  
//  RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads*2);
  SliceManager sm = new SliceManager();
  
  AtomicBoolean stopFlag = new AtomicBoolean(false);
  
  MTExporterStat statistics = new MTExporterStat();
  
  for( OutputTask ot : outputs )
   tPool.submit(ot);

  AtomicLong limitCnt = null;
  
  if( limit > 0 )
   limitCnt = new AtomicLong(limit);
  
  for( int i=0; i < threads; i++ )
  {
   MTSliceExporterTask et = new MTSliceExporterTask(emf, sm, since, tasks, statistics, msgQ, stopFlag, sourcesByName,limitCnt);
   
   exporters.add(et);
   
   tPool.submit( et );
  }
  
  int count=0;
  
  int tnum = threads;
  int tout = outputs.size();
  
  Throwable exception = null;
  
  while(true)
  {
   ControlMessage o;

   try
   {
    o = msgQ.take();
   }
   catch(InterruptedException e)
   {
    continue;
   }

   if(o.getType() == Type.PROCESS_FINISH)
    tnum--;
   else if(o.getType() == Type.OUTPUT_FINISH)
    tout--;

   
   if(tnum == 0 || o.getType() == Type.OUTPUT_ERROR || o.getType() == Type.PROCESS_ERROR)
   {
    exception = o.getException();

    PoisonedObject po = new PoisonedObject();

    stopFlag.set(true);

    for(FormattingTask ft : tasks)
    {
     ft.getGroupQueue().clear();
     putIntoQueue(ft.getGroupQueue(),po);

     if(ft.getSampleQueue() != null)
     {
      ft.getSampleQueue().clear();
      putIntoQueue(ft.getSampleQueue(),po);
     }
    }
    
    break;
   }


   if(tout == 0)
    break;

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
  
  if( exception != null )
   throw exception;
   
  return statistics;
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
 

}
