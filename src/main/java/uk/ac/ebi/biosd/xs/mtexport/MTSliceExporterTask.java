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

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.init.Init;
import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.util.GroupSampleUtil;
import uk.ac.ebi.biosd.xs.util.Slice;
import uk.ac.ebi.biosd.xs.util.SliceManager;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;

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

 private final Double grpMul;
 private final Double smpMul;
 
 private final Logger log = LoggerFactory.getLogger(Init.class);

 
 public MTSliceExporterTask( EntityManagerFactory emf, SliceManager slMgr, long since, List<FormattingTask> tasks,
   MTExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, boolean srcByNm, AtomicLong lim, Double grpMul, Double smpMul )
 {
  emFactory = emf;
  sliceMngr = slMgr;
  this.since = since;
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  this.grpMul = grpMul;
  this.smpMul = smpMul;
  
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
  

  int grpMulFloor = 1;
  double grpMulFrac = 0;
  
  if( grpMul != null )
  {
   grpMulFloor = (int)Math.floor(grpMul);
   grpMulFrac = grpMul-grpMulFloor;
  }

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
     int nRep = grpMulFloor;

     if( grpMul != null && grpMulFrac > 0.005 )
      nRep += Math.random() < grpMulFrac ? 1 : 0;

     for(int grpRep = 1; grpRep <= nRep; grpRep++)
     {

      if(stopFlag.get())
      {
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
       return;
      }

      if(limit != null)
      {
       long c = limit.decrementAndGet();

       if(c < 0)
       {
        putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));

        return;
       }

      }

      BioSampleGroup ng = g;
      
      if( grpMul != null )
       ng = GroupSampleUtil.cloneGroup(g, g.getAcc()+"00"+grpRep, smpMul);

      
      msiTags.clear();
      int nSmp = ng.getSamples().size();

      stat.incGroupCounter();
      stat.addSampleCounter(nSmp);

      if(AbstractXMLFormatter.isGroupPublic(ng, stat.getNowDate()))
       stat.incGroupPublicCounter();

      for(MSI msi : ng.getMSIs())
      {
       for(DatabaseRecordRef db : msi.getDatabaseRecordRefs())
       {
        String scrNm = sourcesByName ? db.getDbName() : db.getAcc();

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


      for(FormattingTask ft : tasks)
      {
       sb.setLength(0);

       ft.getFormatter().exportGroup(ng, sb, false);

       putIntoQueue(ft.getGroupQueue(), sb.toString());

      }

      if(hasSampleOutput)
      {
       for(BioSample s : ng.getSamples())
       {
        if(!stat.addSample(s.getAcc()))
         continue;

        stat.incUniqSampleCounter();

        if(AbstractXMLFormatter.isSamplePublic(s, stat.getNowDate()))
         stat.incSamplePublicUniqCounter();

        for(FormattingTask ft : tasks)
        {
         if(ft.getSampleQueue() == null)
          continue;

         sb.setLength(0);

         ft.getFormatter().exportSample(s, sb, false);

         putIntoQueue(ft.getSampleQueue(), sb.toString());
        }
        
       }
      }

      if(stopFlag.get())
      {
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
       return;
      }

     }}

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