package uk.ac.ebi.biosd.xs.mtexport;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.export.AuxInfo;
import uk.ac.ebi.biosd.xs.mtexport.ControlMessage.Type;
import uk.ac.ebi.biosd.xs.service.AuxInfoImpl;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;

public class SGExporterTask implements MTExportTask
{
 /**
  * 
  */
 public static final int MaxErrorRecoverAttempts=3;
 
// private final SliceManager sliceMngr;
 
 private final EntityManagerFactory myEqFactory;
 private final List<FormattingTask> tasks;
 private final ExporterStat stat;
 private final BlockingQueue<ControlMessage> controlQueue;
 private final AtomicBoolean stopFlag;
 private boolean hasSourcesByName;
 private boolean hasSourcesByAcc;
 private boolean hasGroupedSmp;
 private boolean hasUngroupedSmp;
 private boolean needGroupLoop;
 private final int maxObjsPerThr;
 private final QueryManager sgQM;

 
 private final Logger log = LoggerFactory.getLogger(SGExporterTask.class);

 private int genNo=0;
 private int laneNo;
 private Thread procThread;
 
 public SGExporterTask(  EntityManagerFactory myeqf, QueryManager qMgr, List<FormattingTask> tasks,
   ExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, MTTaskConfig tCfg )
 {
  
  myEqFactory = myeqf;
  
  sgQM = qMgr;
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  
  maxObjsPerThr = tCfg.getItemsPerThreadHardLimit();
  
  stopFlag = stf;
  
  hasGroupedSmp=false;
  hasUngroupedSmp=false;
  hasSourcesByName = false;
  hasSourcesByAcc = false;
  needGroupLoop = false;

 
  
//  sourcesByName = tCfg.isSourcesByName();
//  groupedSmpOnly = tCfg.isGroupedSamplesOnly();
  
  
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

 @Override
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

  procThread.setName(procThread.getName() + "-ExporterTask-gen" + (++genNo) + "-lane" + laneNo);

  AuxInfo auxInf = null;

  if(myEqFactory != null)
   auxInf = new AuxInfoImpl(myEqFactory);

  int objCount = 0;


  try
  {
   StringBuilder sb = new StringBuilder();

   while(true)
   {

    if(needGroupLoop)
    {
     Collection<BioSampleGroup> grps = null;

     int restart = 0;

     while(true)
     {
      try
      {
       grps = sgQM.getGroups();
       break;
      }
      catch(PersistenceException e)
      {
       sgQM.close();

       restart++;

       stat.incRecoverAttempt();

       if(restart > MaxErrorRecoverAttempts)
        throw e;
      }

     }

     if(grps.size() == 0)
     {
      log.debug("({}) No more groups to process", Thread.currentThread().getName());
      needGroupLoop = false;
     }

     for(BioSampleGroup grp : grps)
     {
      if(stopFlag.get())
      {
       log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
       return;
      }

      if(!stat.addGroup(grp.getId()))
       continue;

      objCount++;

      //      System.out.printf("=MSI (L%d-G%d-N%d) %s  Processing group %s %n"
      //        ,laneNo,genNo,msiCount,msi.getAcc(), g.getAcc());

      stat.incGroupCounter();

      boolean grpPub = AbstractXMLFormatter.isGroupPublic(grp, stat.getNowDate());

      if(grpPub)
       stat.incGroupPublicCounter();

      for(FormattingTask ft : tasks)
      {
       if(ft.getGroupQueue() == null)
        continue;

       if(!ft.confirmOutput())
        continue;

       
       restart = 0;

       while(true)
       {
        try
        {
         sb.setLength(0);
         ft.getFormatter().exportGroup(grp, auxInf, sb, false);
         break;
        }
        catch(PersistenceException e)
        {
         restart++;

         stat.incRecoverAttempt();

         if(restart > MaxErrorRecoverAttempts)
          throw e;
        }

       }
       
       putIntoQueue(ft.getGroupQueue(), sb.toString());
      }

      if(hasGroupedSmp || maxObjsPerThr <= 0 || maxObjsPerThr - objCount > sgQM.getChunkSize() )
      {
       for(BioSample s : grp.getSamples())
       {
        if(!hasGroupedSmp && maxObjsPerThr > 0 && maxObjsPerThr - objCount <= sgQM.getChunkSize() )
         break;

        if(!stat.addSample(s.getId()))
         continue;
        
        objCount++;

        countSample(s);
        
        for(FormattingTask ft : tasks)
        {
         if(ft.getSampleQueue() == null)
          continue;

         if(!ft.confirmOutput())
          continue;

        
         restart = 0;

         while(true)
         {
          try
          {
           sb.setLength(0);
           ft.getFormatter().exportSample(s, auxInf, sb, false);
           break;
          }
          catch(PersistenceException e)
          {
           restart++;

           stat.incRecoverAttempt();

           if(restart > MaxErrorRecoverAttempts)
            throw e;
          }

         }
         

         putIntoQueue(ft.getSampleQueue(), sb.toString());
        }

       }
      }

      if(stopFlag.get())
      {
       log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
       return;
      }

     }

    }

    if(hasUngroupedSmp)
    {
     Collection<BioSample> smpls = null;
     
     int restart = 0;

     while(true)
     {
      try
      {
       smpls = sgQM.getSamples();
       break;
      }
      catch(PersistenceException e)
      {
       sgQM.close();

       restart++;

       stat.incRecoverAttempt();

       if(restart > MaxErrorRecoverAttempts)
        throw e;
      }

     }
     

     if(smpls.size() == 0 && !needGroupLoop)
     {
      log.debug("({}) No more data to process", Thread.currentThread().getName());
      break;
     }

     for(BioSample s : smpls)
     {
      
      if(!stat.addSample(s.getId()))
       continue;

      objCount++;

      if(stopFlag.get())
      {
       log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
       putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
       return;
      }

      countSample(s);
      
      for(FormattingTask ft : tasks)
      {
       if(ft.getSampleQueue() == null || ft.isGroupedSamplesOnly())
        continue;

       if(!ft.confirmOutput())
        continue;


       restart = 0;

       while(true)
       {
        try
        {
         sb.setLength(0);
         ft.getFormatter().exportSample(s, auxInf, sb, false);
         break;
        }
        catch(PersistenceException e)
        {
         restart++;

         stat.incRecoverAttempt();

         if(restart > MaxErrorRecoverAttempts)
          throw e;
        }

       }

       putIntoQueue(ft.getSampleQueue(), sb.toString());
      }

     }

    }
    else if(!needGroupLoop)
    {
     log.debug("({}) No more data to process", Thread.currentThread().getName());
     break;
    }

    boolean needMoreData = false;

    for(FormattingTask ft : tasks)
    {
     if(ft.confirmOutput())
     {
      needMoreData = true;
      break;
     }
    }

    if(!needMoreData)
    {
     log.debug("({}) Output tasks don't need more data. Breaking loop", Thread.currentThread().getName());
     break;
    }

    if(maxObjsPerThr > 0 && objCount >= maxObjsPerThr)
    {
     log.debug("({}) Thread TTL expared. Processed {} objects. Sending TTL message", Thread.currentThread().getName(), objCount);
     
     if(auxInf != null)
      auxInf.destroy();
     
     auxInf = null;

     sgQM.close();
     
     putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_TTL, this));
     return;
    }

    if(auxInf != null)
     auxInf.clear();

   }

   log.debug("({}) Thread terminating. Sending FINISH message", Thread.currentThread().getName());
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));

  }
  catch(Throwable e)
  {
   e.printStackTrace();

   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_ERROR, this, e));
  }
  finally
  {
   if(auxInf != null)
    auxInf.destroy();

   sgQM.close();
  }

 }
  

 void countSample( BioSample s )
 {
  stat.incUniqSampleCounter();

  if(AbstractXMLFormatter.isSamplePublic(s, stat.getNowDate()))
   stat.incSamplePublicUniqCounter();
  
  if(hasSourcesByName || hasSourcesByAcc)
  {

   for(MSI gmsi : s.getMSIs())
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
        stat.addToSourceByAcc(scrNm, 1);
      }

     }

     if(hasSourcesByName)
     {
      String scrNm = db.getDbName();

      if(scrNm != null)
      {
       scrNm = scrNm.trim();

       if(scrNm.length() != 0)
        stat.addToSourceByAcc(scrNm, 1);
      }
     }
    }
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