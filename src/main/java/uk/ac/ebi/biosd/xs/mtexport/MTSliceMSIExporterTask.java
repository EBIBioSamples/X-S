package uk.ac.ebi.biosd.xs.mtexport;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

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

public class MTSliceMSIExporterTask implements Runnable
{
 /**
  * 
  */
 public static final int MaxErrorRecoverAttempts=3;
 
 private final EntityManagerFactory emFactory;
 private final EntityManagerFactory myEqFactory;
 private final SliceManager msiSliceMngr;
 private final long since;
 private final List<FormattingTask> tasks;
 private final ExporterStat stat;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final AtomicBoolean stopFlag;
 private boolean hasSourcesByName;
 private boolean hasSourcesByAcc;
 private boolean hasGroupedSmp;
 private boolean hasUngroupedSmp;
 private boolean needGroupLoop;
 private final int maxMSIs;

 private int genNo=0;
 
 private final Double grpMul;
 private final Double smpMul;
 
 private final Logger log = LoggerFactory.getLogger(Init.class);
 
 private int laneNo;


 private Thread procThread;

 

 public MTSliceMSIExporterTask( EntityManagerFactory emf, EntityManagerFactory myeqf, SliceManager msiSlMgr, List<FormattingTask> tasks,
   ExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, MTTaskConfig tCfg )
 {
  
  emFactory = emf;
  myEqFactory = myeqf;
  
  msiSliceMngr = msiSlMgr;
  
  this.since = tCfg.getSince();
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  this.grpMul = tCfg.getGroupMultiplier();
  this.smpMul = tCfg.getSampleMultiplier();
  
  maxMSIs = tCfg.getMaxItemsPerThread();
  
  stopFlag = stf;
  
  hasGroupedSmp=false;
  hasUngroupedSmp=false;
  hasSourcesByName = false;
  hasSourcesByAcc = false;
  needGroupLoop = false;
  
 
  
  for( FormattingTask ft : tasks )
  {
   
   if( ft.getSampleQueue() != null )
   {
    if( ft.isGroupedSamplesOnly() )
     hasGroupedSmp = true;
    else
     hasUngroupedSmp = true;
   }
   
   if( ft.getGroupQueue() != null || hasGroupedSmp )
    needGroupLoop = true;

   
   if( ft.isSourcesByAcc() )
    hasSourcesByAcc = true;

   if( ft.isSourcesByName() )
    hasSourcesByName = true;
  }
  
 }

 public Thread getProcessingThread()
 {
  return procThread;
 }

 public int getLaneNo()
 {
  return laneNo;
 }

 public void setLaneNo(int laneNo)
 {
  this.laneNo = laneNo;
 }
 
 @Override
 public void run()
 {
  procThread = Thread.currentThread();
  
  procThread.setName(procThread.getName()+"-ExporterTask-gen"+(++genNo)+"-lane"+laneNo);
  
  MSISliceQueryManager msiQM = null;

  AuxInfo auxInf = null;

  if(myEqFactory != null)
   auxInf = new AuxInfoImpl(myEqFactory);

  int msiCount=0;
  
  try
  {
   int grpMulFloor = 1;
   double grpMulFrac = 0;

   msiQM = new MSISliceQueryManager(emFactory, since);

   if( needGroupLoop )
   {

    if(grpMul != null)
    {
     grpMulFloor = (int) Math.floor(grpMul);
     grpMulFrac = grpMul - grpMulFloor;
    }

   }

   Set<String> msiTags = new HashSet<String>();

   StringBuilder sb = new StringBuilder();

   mainLoop: while(true)
   {
    Slice sl = msiSliceMngr.getSlice();


    try
    {
     Collection<MSI> msis = null;
     
     int restart=0;
     
     while( true )
     {
      try
      {
       msis = msiQM.getMSIs(sl);
       break;
      }
      catch( PersistenceException e )
      {
       msiQM.release();

       restart++;
       
       stat.incRecoverAttempt();
       
       if( restart > MaxErrorRecoverAttempts)
        throw e;
      }
      
     }
     
     
     if( log.isDebugEnabled() )
      log.debug("({}) Processing slice: {}, size: {}", new Object[] { Thread.currentThread().getName(), sl, msis.size() });

     if(msis.size() == 0)
     {
      log.debug("({}) No more data to process", Thread.currentThread().getName());
      break;
     }

     for(MSI msi : msis)
     {
      if(stopFlag.get())
      {
       log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
       return;
      }
      
      boolean didOutput=false;
      
      msiCount++;

            
//      long time = System.currentTimeMillis();
//      System.out.printf("+MSI (L%d-G%d-N%d) %s  Samples: %d Groups: %d Mem: %.2fG%n"
//        ,laneNo,genNo,msiCount,msi.getAcc(),msi.getSamples().size(),msi.getSampleGroups().size(),(double)Runtime.getRuntime().freeMemory()/1024/1024/1024);
//     
//      int grpAnnt=0;
//      int grpSmp=0;
//      int smpAnnt=0;
//      
//      for( BioSampleGroup g : msi.getSampleGroups() )
//      {
//       grpAnnt += g.getPropertyValues().size();
//       grpSmp += g.getSamples().size();
//      }
//      
//      for( BioSample s : msi.getSamples() )
//       smpAnnt += s.getPropertyValues().size();
//      
//      System.out.printf("=MSI (L%d-G%d-N%d) %s  Sample annts: %d Group annts: %d Group samples: %d %n"
//        ,laneNo,genNo,msiCount,msi.getAcc(),smpAnnt, grpAnnt, grpSmp);

      
      stat.incMSICounter();

      if(needGroupLoop)
      {

       for(BioSampleGroup g : msi.getSampleGroups())
       {
        if( ! stat.addGroup(g.getId()) )
         continue;
        
//        System.out.printf("=MSI (L%d-G%d-N%d) %s  Processing group %s %n"
//          ,laneNo,genNo,msiCount,msi.getAcc(), g.getAcc());

        
        int nRep = grpMulFloor;

        if(grpMul != null && grpMulFrac > 0.005)
         nRep += Math.random() < grpMulFrac ? 1 : 0;

        for(int grpRep = 1; grpRep <= nRep; grpRep++)
        {

         if(stopFlag.get())
         {
          log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
          putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
          return;
         }

//         grpCount++;

         BioSampleGroup ng = g;

         if(grpMul != null)
          ng = GroupSampleUtil.cloneGroup(g, g.getAcc() + "00" + grpRep, smpMul);

         msiTags.clear();
         int nSmp = ng.getSamples().size();

         stat.incGroupCounter();
         stat.addSampleCounter(nSmp);

         boolean grpPub = AbstractXMLFormatter.isGroupPublic(ng, stat.getNowDate());

         if(grpPub)
          stat.incGroupPublicCounter();

         if(hasSourcesByName || hasSourcesByAcc)
         {

          for(MSI gmsi : ng.getMSIs())
          {
           for(DatabaseRecordRef db : gmsi.getDatabaseRecordRefs())
           {
            if(hasSourcesByAcc)
            {
             String scrNm = db.getAcc();

             if(scrNm != null)
             {
              scrNm = scrNm.trim();

              if(scrNm.length() != 0)
               stat.addToSourceByAcc(scrNm, nSmp);
             }

            }

            if(hasSourcesByName)
            {
             String scrNm = db.getDbName();

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
          if(ft.getGroupQueue() == null)
           continue;
          
          if( ! ft.confirmOutput() )
           continue;

          didOutput = true;
          
          sb.setLength(0);

          ft.getFormatter().exportGroup(ng, auxInf, sb, false);

          putIntoQueue(ft.getGroupQueue(), sb.toString());
         }

         if(hasGroupedSmp)
         {
          for(BioSample s : ng.getSamples())
          {
           if(!stat.addSample(s.getId()))
            continue;

           if(!hasUngroupedSmp)
           {
            stat.incUniqSampleCounter();

            if(AbstractXMLFormatter.isSamplePublic(s, stat.getNowDate()))
             stat.incSamplePublicUniqCounter();
           }

           for(FormattingTask ft : tasks)
           {
            if(ft.getSampleQueue() == null || !ft.isGroupedSamplesOnly())
             continue;
            
            if(!ft.confirmOutput())
             continue;
           
            didOutput = true;

            sb.setLength(0);

            ft.getFormatter().exportSample(s, auxInf, sb, false);

            putIntoQueue(ft.getSampleQueue(), sb.toString());
           }

//           msiQM.detach(s);
          }
         }

         if(stopFlag.get())
         {
          log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
          putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
          return;
         }

        }
       
//        msiQM.detach(g);
       }

      }

      if(hasUngroupedSmp)
      {
       int smpMulFloor = 1;
       double smpMulFrac = 0;

       if(smpMul != null)
       {
        smpMulFloor = (int) Math.floor(smpMul);
        smpMulFrac = smpMul - smpMulFloor;
       }

       for(BioSample s : msi.getSamples())
       {
        if(!stat.addSample(s.getId()))
         continue;

        int nSmpRep = smpMulFloor;

        if(smpMul != null && smpMulFrac > 0.005)
         nSmpRep += Math.random() < smpMulFrac ? 1 : 0;

        for(int smpRep = 1; smpRep <= nSmpRep; smpRep++)
        {

         if(stopFlag.get())
         {
          log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
          putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
          return;
         }

         BioSample ns = s;

         if(smpMul != null)
          ns = GroupSampleUtil.cloneSample(ns, s.getAcc() + "00" + smpRep);

         stat.incUniqSampleCounter();

         if(AbstractXMLFormatter.isSamplePublic(s, stat.getNowDate()))
          stat.incSamplePublicUniqCounter();

         for(FormattingTask ft : tasks)
         {
          if(ft.getSampleQueue() == null || ft.isGroupedSamplesOnly())
           continue;

          if( ! ft.confirmOutput() )
           continue;
          
          didOutput = true;
          
          sb.setLength(0);

          ft.getFormatter().exportSample(ns, auxInf, sb, false);

          putIntoQueue(ft.getSampleQueue(), sb.toString());
         }

        }

//        msiQM.detach(s);
       }

      }

      
      if( ! didOutput )
       log.info("Suspicious MSI - no output: "+msi.getAcc()+" Groups: "+msi.getSampleGroups().size()+" Samples: "+msi.getSamples().size());
      
      boolean needMoreData = false;
      
      for(FormattingTask ft : tasks)
      {
       if( ft.confirmOutput() )
       {
        needMoreData = true;
        break;
       }
      }
      
      if( ! needMoreData )
      {
       log.debug("({}) Output tasks don't need more data. Breaking loop", Thread.currentThread().getName());
       break mainLoop;
      }
//      System.out.printf("-MSI (L%d-G%d-N%d) %s  Samples: %d Groups: %d Time: %d Mem: %.2fG%n"
//        ,laneNo,genNo,msiCount,msi.getAcc(),msi.getSamples().size(),msi.getSampleGroups().size(),System.currentTimeMillis()-time,(double)Runtime.getRuntime().freeMemory()/1024/1024/1024);
      
      
//      msiQM.detach(msi);
     }
     
     if( maxMSIs > 0 && msiCount >= maxMSIs )
     {
      log.debug("({}) Thread TTL expared. Processed {} MSIs. Sending TTL message", Thread.currentThread().getName(), msiCount);
      putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_TTL, this));
      return;
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
     msiQM.release();
     
     if( auxInf != null )
      auxInf.clear();
    }

   }
   
   log.debug("({}) Thread terminating. Sending FINISH message", Thread.currentThread().getName());
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
  }
  finally
  {
   if(auxInf != null)
    auxInf.destroy();
  
   if( msiQM != null )
    msiQM.close();
   
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