package uk.ac.ebi.biosd.xs.mtexport;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.init.Init;
import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.util.Slice;
import uk.ac.ebi.biosd.xs.util.SliceManager;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class MTSliceExporterTask implements Runnable
{
 /**
  * 
  */
 private final EntityManagerFactory emFactory;
 private final SliceManager sliceMngr;
 private final long since;
 private final List<FormattingTask> tasks;
 private final MTExporterStat stat;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final AtomicBoolean stopFlag;
 private final boolean sourcesByName;
 private boolean hasSampleOutput = false;
 private final AtomicLong limit;
 
 private final Logger log = LoggerFactory.getLogger(Init.class);

 
 public MTSliceExporterTask( EntityManagerFactory emf, SliceManager slMgr, long since, List<FormattingTask> tasks,
   MTExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, boolean srcByNm, AtomicLong lim )
 {
  emFactory = emf;
  sliceMngr = slMgr;
  this.since = since;
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  stopFlag = stf;
  
  sourcesByName = srcByNm;
  
  limit = lim;
  
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
  GroupSliceQueryManager grpq = new GroupSliceQueryManager(emFactory);
  



  Set<String> msiTags = new HashSet<String>();

  StringBuilder sb = new StringBuilder();

  while( true )
  {
   Slice sl = sliceMngr.getSlice();
   
   log.debug("("+Thread.currentThread().getName()+") Processing slice: " + sl);

   try
   {

    Collection<BioSampleGroup> grps=grpq.getGroups( since, sl );
    
    if( grps.size() == 0  )
    {
     putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );

     return;
    }
    
    for(BioSampleGroup g : grps)
    {
     if(stopFlag.get())
     {
      putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );
      return;
     }
     
     if( limit != null )
     {
      long c = limit.decrementAndGet();
      
      if( c < 0 )
      {
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );

       return;
      }

     }
     
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
   catch(Throwable e)
   {
    e.printStackTrace();

    putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_ERROR,this,e) );

    return;
   }
   finally
   {
    grpq.release();
   }

   if( limit != null && limit.get() < 0 )
   {
    putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH,this) );
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