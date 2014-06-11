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
import uk.ac.ebi.biosd.xs.export.AuxInfo;
import uk.ac.ebi.biosd.xs.init.Init;
import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.service.AuxInfoImpl;
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
 private final EntityManagerFactory myEqFactory;
 private final SliceManager grpSliceMngr;
 private final SliceManager smpSliceMngr;
 private final long since;
 private final List<FormattingTask> tasks;
 private final ExporterStat stat;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final AtomicBoolean stopFlag;
 private boolean hasSourcesByName;
 private boolean hasSourcesByAcc;
 private boolean hasGroupedSmp;
 private boolean hasUngroupedSmp;
 private final AtomicLong limit;

 private final Double grpMul;
 private final Double smpMul;
 
 private final Logger log = LoggerFactory.getLogger(Init.class);

 
 public MTSliceExporterTask( EntityManagerFactory emf, EntityManagerFactory myeqf, SliceManager grpSlMgr, SliceManager smpSlMgr, List<FormattingTask> tasks,
   ExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, AtomicLong lim, MTTaskConfig tCfg )
 {
  emFactory = emf;
  myEqFactory = myeqf;
  
  grpSliceMngr = grpSlMgr;
  smpSliceMngr = smpSlMgr;
  
  this.since = tCfg.getSince();
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  this.grpMul = tCfg.getGroupMultiplier();
  this.smpMul = tCfg.getSampleMultiplier();
  
  stopFlag = stf;
  
  hasGroupedSmp=false;
  hasUngroupedSmp=false;
  hasSourcesByName = false;
  hasSourcesByAcc = false;
  
  
//  sourcesByName = tCfg.isSourcesByName();
//  groupedSmpOnly = tCfg.isGroupedSamplesOnly();
  
  limit = lim;
  
  for( FormattingTask ft : tasks )
  {
   if( ft.getSampleQueue() != null )
   {
    if( ft.isGroupedSamplesOnly() )
     hasGroupedSmp = true;
    else
     hasUngroupedSmp = true;
   }
   

   
   if( ft.isSourcesByAcc() )
    hasSourcesByAcc = true;

   if( ft.isSourcesByName() )
    hasSourcesByName = true;
  }
  
 }

 @Override
 public void run()
 {
  GroupSliceQueryManager grpq = new GroupSliceQueryManager(emFactory);

  AuxInfo auxInf = null;

  if(myEqFactory != null)
   auxInf = new AuxInfoImpl(myEqFactory);

  try
  {

   int grpMulFloor = 1;
   double grpMulFrac = 0;

   if(grpMul != null)
   {
    grpMulFloor = (int) Math.floor(grpMul);
    grpMulFrac = grpMul - grpMulFloor;
   }

   Set<String> msiTags = new HashSet<String>();

   StringBuilder sb = new StringBuilder();

   while(true)
   {
    Slice sl = grpSliceMngr.getSlice();

    log.debug("(" + Thread.currentThread().getName() + ") Processing slice: " + sl);

    try
    {

     Collection<BioSampleGroup> grps = grpq.getGroups(since, sl);

     if(grps.size() == 0)
      break;

     for(BioSampleGroup g : grps)
     {
      int nRep = grpMulFloor;

      if(grpMul != null && grpMulFrac > 0.005)
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

       if(grpMul != null)
        ng = GroupSampleUtil.cloneGroup(g, g.getAcc() + "00" + grpRep, smpMul);

       msiTags.clear();
       int nSmp = ng.getSamples().size();

       stat.incGroupCounter();
       stat.addSampleCounter(nSmp);

       if(AbstractXMLFormatter.isGroupPublic(ng, stat.getNowDate()))
        stat.incGroupPublicCounter();

       if( hasSourcesByName || hasSourcesByAcc )
       {
       
        for(MSI msi : ng.getMSIs())
        {
         for(DatabaseRecordRef db : msi.getDatabaseRecordRefs())
         {
          if( hasSourcesByAcc )
          {
           String scrNm =db.getAcc();

           if(scrNm != null)
           {
            scrNm = scrNm.trim();
            
            if(scrNm.length() != 0)
             stat.addToSourceByAcc(scrNm, nSmp);
            
           }
        
          }
          
          if( hasSourcesByName )
          {
           String scrNm =db.getDbName();

           if(scrNm != null)
           {
            scrNm = scrNm.trim();
            
            if(scrNm.length() != 0)
             stat.addToSourceByAcc(scrNm, nSmp);
           }
        
          }
          
         }
        }
        
       }

       for(FormattingTask ft : tasks)
       {
        sb.setLength(0);

        ft.getFormatter().exportGroup(ng, auxInf, sb, false);

        putIntoQueue(ft.getGroupQueue(), sb.toString());

       }

       if( hasGroupedSmp )
       {
        for(BioSample s : ng.getSamples())
        {
         if(!stat.addSample(s.getAcc()))
          continue;

         if( ! hasUngroupedSmp )
         {
          stat.incUniqSampleCounter();

          if(AbstractXMLFormatter.isSamplePublic(s, stat.getNowDate()))
           stat.incSamplePublicUniqCounter();
         }
         
         for(FormattingTask ft : tasks)
         {
          if(ft.getSampleQueue() == null || ! ft.isGroupedSamplesOnly() )
           continue;

          sb.setLength(0);

          ft.getFormatter().exportSample(s, auxInf, sb, false);

          putIntoQueue(ft.getSampleQueue(), sb.toString());
         }

        }
       }

       if(stopFlag.get())
       {
        putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
        return;
       }

      }
     }

    }
    catch(Throwable e)
    {
     e.printStackTrace();

     putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_ERROR, this, e));

     return;
    }
    finally
    {
     grpq.release();
    }

    if(limit != null && limit.get() < 0)
    {
     putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
     return;
    }

   }
   


   if(hasUngroupedSmp)
   {
    SampleSliceQueryManager smpq = new SampleSliceQueryManager(emFactory);

    while(true)
    {
     Slice sl = smpSliceMngr.getSlice();

     log.debug("(" + Thread.currentThread().getName() + ") Processing slice: " + sl);

     try
     {

      Collection<BioSample> smps = smpq.getSamples(since, sl);

      if(smps.size() == 0)
       break;

      for(BioSample s : smps)
      {
       if( ! hasGroupedSmp )
       {
        stat.incUniqSampleCounter();

        if(AbstractXMLFormatter.isSamplePublic(s, stat.getNowDate()))
         stat.incSamplePublicUniqCounter();
       }

       for(FormattingTask ft : tasks)
       {
        if(ft.getSampleQueue() == null || ft.isGroupedSamplesOnly() )
         continue;

        sb.setLength(0);

        ft.getFormatter().exportSample(s, auxInf, sb, false);

        putIntoQueue(ft.getSampleQueue(), sb.toString());
       }
      }
     }
     catch(Throwable e)
     {
      e.printStackTrace();

      putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_ERROR, this, e));

      return;
     }
     finally
     {
      smpq.release();
     }

    }

   }
   
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
   
  }
  finally
  {
   if(auxInf != null)
    auxInf.destroy();
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