package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import javax.persistence.Query;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.RangeManager;
import uk.ac.ebi.biosd.xs.util.RangeManager.Range;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class ExporterMT implements Exporter
{
 private final EntityManagerFactory emf;
 private final AbstractXMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 private final int threads;
 
 public ExporterMT(EntityManagerFactory emf, AbstractXMLFormatter formatter, boolean exportSources, boolean sourcesByName, int thN)
 {
  super();
  this.emf = emf;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  threads = thN;
 }

 
 @Override
 public void export( long since, Appendable out, long limit) throws IOException
 {
  ExecutorService tPool = Executors.newFixedThreadPool(threads);
  
  BlockingQueue<Object> reqQ = new ArrayBlockingQueue<>(100);
  RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads);

  Map<String, Counter> srcMap = new HashMap<String, Counter>();
  
  
  AtomicBoolean stopFlag = new AtomicBoolean(false);
  
  for( int i=0; i < threads; i++ )
   tPool.submit( new GroupExporterTask(srcMap, rm, emf, since, reqQ, stopFlag) );
  
  formatter.exportHeader( since, out );
  
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
 
 
 enum TaskState
 {
  OK,
  IOERROR,
  INTERRUPTED
 }
 
 class GroupExporterTask implements Callable<TaskState>
 {
  private final RangeManager rangeMngr;
  private final EntityManagerFactory emFactory;
  private final Map<String, Counter> sourcesMap;
  private final BlockingQueue<Object> resultQueue;
  private final AtomicBoolean stopFlag;
  private final long since;
  
  GroupExporterTask(Map<String, Counter> srcMap, RangeManager rMgr, EntityManagerFactory emf, long since, BlockingQueue<Object> resQ, AtomicBoolean stf )
  {
   sourcesMap = srcMap;
   rangeMngr = rMgr;
   
   emFactory = emf;
   resultQueue = resQ;
   
   stopFlag = stf;
   
   this.since = since;
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

     for(BioSampleGroup g : grpq.getGroups( since, r.getMin(), r.getMax() ))
     {
      if(stopFlag.get())
       return TaskState.INTERRUPTED;

      lastId = g.getId();
      
      sb.setLength(0);

      formatter.exportGroup(g, sb);

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
 
 
 class SampleExporterTask implements Runnable
 {
  RangeManager rangeMngr;
  Query listQuery;
  BlockingQueue<Object> resultQueue;
  AtomicBoolean stopFlag;
  
  SampleExporterTask( RangeManager rMgr, Query q, BlockingQueue<Object> resQ, AtomicBoolean stf )
  {
   rangeMngr = rMgr;
   
   listQuery = q;
   resultQueue = resQ;
   
   stopFlag = stf;
  }

  @Override
  public void run()
  {
   Range r = rangeMngr.getRange();

   long lastId = 0;

   StringBuilder sb = new StringBuilder();

   try
   {

    while(r != null)
    {
     System.out.println("Processing range: " + r + " Thread: " + Thread.currentThread().getName());

     lastId = r.getMax();
     listQuery.setParameter(1, r.getMin());
     listQuery.setParameter(2, r.getMax());

     @SuppressWarnings("unchecked")
     List<BioSample> result = listQuery.getResultList();

     for(BioSample s : result)
     {
      if( stopFlag.get() )
       return;
      
      sb.setLength(0);

      formatter.exportSample(s, sb);

      putIntoQueue(sb.toString());
      
      if( stopFlag.get() )
       return;

     }
     
     r.setMin(lastId + 1);

     r = rangeMngr.returnAndGetRange(r);

     if(r == null)
     {
      putIntoQueue( new PoisonedObject() );
      
//      System.out.println("Processing finished. Thread: " + Thread.currentThread().getName());

      return;
     }
    }
   }
   catch(IOException e)
   {
    e.printStackTrace();
    
    putIntoQueue( new PoisonedObject(e) );

   }
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

}
