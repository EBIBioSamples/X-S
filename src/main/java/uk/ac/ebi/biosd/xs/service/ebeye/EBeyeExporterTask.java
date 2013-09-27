package uk.ac.ebi.biosd.xs.service.ebeye;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.service.GroupQueryManager;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.RangeManager;
import uk.ac.ebi.biosd.xs.util.RangeManager.Range;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class EBeyeExporterTask implements Runnable
{
 private final RangeManager rangeMngr;
 private final EntityManagerFactory emFactory;
 private final Map<String, Counter> sourcesMap;
 private final BlockingQueue<Object> resultQueue;
 private final AtomicBoolean stopFlag;
 
 EBeyeExporterTask(Map<String, Counter> srcMap, RangeManager rMgr, EntityManagerFactory emf,  BlockingQueue<Object> resQ, AbstractXMLFormatter ebeyeFmt, AbstractXMLFormatter auxFmt, AtomicBoolean stf )
 {
  sourcesMap = srcMap;
  rangeMngr = rMgr;
  
  emFactory = emf;
  resultQueue = resQ;
  
  stopFlag = stf;
  
 }

 @Override
 public void run()
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
      return;

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
      return;

    }

   }
   catch(IOException e)
   {
    e.printStackTrace();

    putIntoQueue(new PoisonedObject(e));

    return;
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

    return;
   }
  }

 
  return;
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
