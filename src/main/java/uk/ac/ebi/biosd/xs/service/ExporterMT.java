package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.sql.Date;
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

public class ExporterMT
{
 private final EntityManagerFactory emf;
 private final AbstractXMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 private final int blockSize;
 private final long limit;
 
 public ExporterMT(EntityManagerFactory emf, AbstractXMLFormatter formatter, boolean exportSources, boolean sourcesByName, int blockSize, long limit)
 {
  super();
  this.emf = emf;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  this.blockSize = blockSize;
  this.limit = limit;
 }

 
 public void export( long since, Appendable out, int threads) throws IOException
 {
  ExecutorService tPool = Executors.newFixedThreadPool(threads);
  
  BlockingQueue<Object> reqQ = new ArrayBlockingQueue<>(100);
  RangeManager rm = new RangeManager(Long.MIN_VALUE,Long.MAX_VALUE,threads);

  Map<String, Counter> srcMap = new HashMap<String, Counter>();
  
  List<EntityManager> emlst = new ArrayList<>(threads);
  
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
   
   tPool.submit( new ExporterTask(srcMap, rm, listQuery, reqQ) );
   
   emlst.add(em);
  }
  
  formatter.exportHeader(new java.util.Date().getTime(), since, out);

  
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
    threads--;
   
   if( threads == 0 )
    break;
   }
   
   out.append(s);
  }

  for( EntityManager em : emlst )
   em.close();
  
  
   
  if( exportSources )
   formatter.exportSources(srcMap, out);
  
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
  
  ExporterTask(Map<String, Counter> srcMap, RangeManager rMgr, Query q, BlockingQueue<Object> resQ )
  {
   sourcesMap = srcMap;
   rangeMngr = rMgr;
   
   listQuery = q;
   resultQueue = resQ;
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
