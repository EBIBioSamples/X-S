package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.RangeManager;
import uk.ac.ebi.biosd.xs.util.RangeManager.Range;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class ExporterMT implements Exporter
{
 private final EntityManagerFactory emf;
 private final AbstractXMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 private final int blockSize;
 private final int threads;
 
 public ExporterMT(EntityManagerFactory emf, AbstractXMLFormatter formatter, boolean exportSources, boolean sourcesByName, int blockSize, int thN)
 {
  super();
  this.emf = emf;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  this.blockSize = blockSize;
  threads = thN;
 }

 
 @Override
 public void export( long since, Appendable out, long limit) throws IOException
 {
  ExecutorService tPool = Executors.newFixedThreadPool(threads);
  
  BlockingQueue<Object> reqQ = new ArrayBlockingQueue<>(100);
  RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads);

  Map<String, Counter> srcMap = new HashMap<String, Counter>();
  
  List<EntityManager> emlst = new ArrayList<>(threads);
  
  AtomicBoolean stopFlag = new AtomicBoolean(false);
  
  for( int i=0; i < threads; i++ )
  {
   EntityManager em = emf.createEntityManager();
   
   Query listQuery;
   
   if( since < 0 )
    listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=?1 and a.id <=?2 ORDER BY a.id");
   else
   {
    listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=?1 and grp.id <=?2 and msi.updateDate > ?3  ORDER BY grp.id");
   
    listQuery.setParameter(3, new Date(since));
   }
   
   listQuery.setMaxResults ( blockSize ); 
   
   tPool.submit( new ExporterTask(srcMap, rm, listQuery, reqQ, stopFlag) );
   
   emlst.add(em);
  }
  
  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  java.util.Date startTime = new java.util.Date();
  long startTs = startTime.getTime();
  
  formatter.exportHeader( startTs, since, out);
  
  out.append("\n<!-- Start time: "+simpleDateFormat.format(startTime)+" -->\n");

  
  int count=0;
  
  int tnum = threads;
  
  while( true )
  {
   Object o;
   
   try
   {
    o=reqQ.take();
   }
   catch(InterruptedException e)
   {
    continue;
   }
   
   if( o == null )
    continue;
   
   String s = o.toString();
   
   if( s == null )
   {
    tnum--;
   
   if( tnum == 0 )
    break;
   }
   
   count++;
   
   out.append(s);
   
   if( limit > 0 && count >= limit )
   {
    stopFlag.set(true);
    break;
   }
  }

  for( EntityManager em : emlst )
   em.close();
  
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
  
  java.util.Date endTime = new java.util.Date();
  long endTs = endTime.getTime();

  long rate = (endTs-startTs)/count;
  
  out.append("\n<!-- Exported: "+count+" groups. Rate: "+rate+"ms per group -->\n<!-- End time: "+simpleDateFormat.format(endTime)+" -->\n");
  
  formatter.exportFooter(out);

 }

 
 static class PoisonObject
 {
  @Override
  public String toString()
  {
   return null;
  }
 }
 
 class ExporterTask implements Runnable
 {
  RangeManager rangeMngr;
  Query listQuery;
  Map<String, Counter> sourcesMap;
  BlockingQueue<Object> resultQueue;
  AtomicBoolean stopFlag;
  
  ExporterTask(Map<String, Counter> srcMap, RangeManager rMgr, Query q, BlockingQueue<Object> resQ, AtomicBoolean stf )
  {
   sourcesMap = srcMap;
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

   Set<String> msiTags = new HashSet<String>();

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
     List<BioSampleGroup> result = listQuery.getResultList();

     for(BioSampleGroup g : result)
     {
      if( stopFlag.get() )
       return;
      
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
     }
     
     r.setMin(lastId + 1);

     r = rangeMngr.returnAndGetRange(r);

     if(r == null)
     {
      putIntoQueue( new PoisonObject() );
      
//      System.out.println("Processing finished. Thread: " + Thread.currentThread().getName());

      return;
     }
    }
   }
   catch(IOException e)
   {
    // TODO Auto-generated catch block
    e.printStackTrace();
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
     continue;
    }
    
    return;
   }

  }
 }
 
}
