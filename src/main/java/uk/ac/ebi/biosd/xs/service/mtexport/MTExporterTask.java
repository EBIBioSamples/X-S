package uk.ac.ebi.biosd.xs.service.mtexport;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.service.GroupQueryManager;
import uk.ac.ebi.biosd.xs.service.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.util.RangeManager;
import uk.ac.ebi.biosd.xs.util.RangeManager.Range;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class MTExporterTask implements Runnable
{
 /**
  * 
  */
 private final EntityManagerFactory emFactory;
 private final RangeManager rangeMngr;
 private final long since;
 private final List<FormattingTask> tasks;
 private final MTExporterStat stat;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final AtomicBoolean stopFlag;
 private final boolean sourcesByName;
 private boolean hasSampleOutput = false;
 
 public MTExporterTask( EntityManagerFactory emf, RangeManager rMgr, long since, List<FormattingTask> tasks,
   MTExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, boolean srcByNm )
 {
  emFactory = emf;
  rangeMngr = rMgr;
  this.since = since;
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  stopFlag = stf;
  
  sourcesByName = srcByNm;
  
  for( FormattingTask ft : tasks )
  {
   if( ft.getSampleQueue() != null )
   {
    hasSampleOutput = true;
    break;
   }
  }
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

    for(BioSampleGroup g : grpq.getGroups( since, r.getMin(), r.getMax() ))
    {
     if(stopFlag.get())
     {
      putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );
      return;
     }
     
     lastId = g.getId();
     
     msiTags.clear();
     int nSmp = g.getSamples().size();
     
     stat.incGroupCounter();
     stat.addSampleCounter(g.getSamples().size());
     
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

       stat.addToSource(scrNm, nSmp);

      }
     }
     
     for( FormattingTask ft : tasks )
     {
      sb.setLength(0);

      ft.getFormatter().exportGroup(g, sb, false);
      
      putIntoQueue(ft.getGroupQueue(), sb.toString());
      
      if( ft.getSampleQueue() != null )
      {}
      
     }
     
     if( hasSampleOutput )
     {
      for( BioSample s : g.getSamples() )
      {
       if( ! stat.addSample( s.getAcc() ) )
        continue;
       
       stat.incUniqSampleCounter();
       
       for( FormattingTask ft : tasks )
       {
        if( ft.getSampleQueue() == null )
         continue;
        
        sb.setLength(0);

        ft.getFormatter().exportSample(s, sb, false);
        
        putIntoQueue(ft.getSampleQueue(), sb.toString());
       }
      }
     }



     if(stopFlag.get())
     {
      putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );
      return;
     }

    }

   }
   catch(IOException e)
   {
    e.printStackTrace();

    putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_ERROR,this,e) );

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
    putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );

    //      System.out.println("Processing finished. Thread: " + Thread.currentThread().getName());

    return;
   }
  }

 
 }

 public <T> void  putIntoQueue( BlockingQueue<T> queue, T o )
 {

  while(true)
  {
   try
   {
    queue.put(o);
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