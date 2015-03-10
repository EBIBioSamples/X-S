package uk.ac.ebi.biosd.xs.mtexport;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;

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

public class IDPrefetchExporterTask implements MTExportTask
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
 private boolean needGroupLoop;
 private final int maxObjsPerThrSoft;
 private final int maxObjsPerThrHard;
 private final IDPrefetchQueryManager sgQM;

 
 private final Logger log = LoggerFactory.getLogger(IDPrefetchExporterTask.class);

 private int genNo=0;
 private int laneNo;
 private Thread procThread;
 
 private int objectCount = 0;

 public IDPrefetchExporterTask(  EntityManagerFactory myeqf, IDPrefetchQueryManager qMgr, List<FormattingTask> tasks,
   ExporterStat stat, BlockingQueue<ControlMessage> controlQueue, AtomicBoolean stf, MTTaskConfig tCfg )
 {
  
  myEqFactory = myeqf;
  
  sgQM = qMgr;
  
  this.tasks = tasks;
  this.stat = stat;
  this.controlQueue = controlQueue;

  maxObjsPerThrSoft = tCfg.getItemsPerThreadSoftLimit();
  maxObjsPerThrHard = tCfg.getItemsPerThreadHardLimit();
  
  stopFlag = stf;
  
  hasSourcesByName = false;
  hasSourcesByAcc = false;
  needGroupLoop = false;

  
//  sourcesByName = tCfg.isSourcesByName();
//  groupedSmpOnly = tCfg.isGroupedSamplesOnly();
  
  
  for( FormattingTask ft : tasks )
  {
   
   if( ft.getGroupQueue() != null )
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
 
 
 private boolean formatSample( BioSample s, AuxInfo auxInf, StringBuilder sb ) throws IOException
 {
  boolean needMoreData = false;
  
  for(FormattingTask ft : tasks)
  {

   if(!ft.confirmOutput())
    continue;

   needMoreData = true;
   
   if(ft.getSampleQueue() == null)
    continue;

   int restart = 0;

   while(true)
   {
    try
    {
     sb.setLength(0);
     ft.getFormatter().exportSample(s, auxInf, sb, false);
     break;
    }
    catch(Exception e)
    {
     restart++;

     stat.incRecoverAttempt();

     if(restart > MaxErrorRecoverAttempts)
      throw e;
    }

   }
   

   putIntoQueue(ft.getSampleQueue(), sb.toString());
  }
  
  return needMoreData;
 }
 
 private boolean formatGroup( BioSampleGroup grp, AuxInfo auxInf, StringBuilder sb ) throws IOException
 {
  boolean needMoreData = false;
  
  for(FormattingTask ft : tasks)
  {
   if(!ft.confirmOutput())
    continue;

   needMoreData = true;
   
   if(ft.getGroupQueue() == null)
    continue;
   
   int restart = 0;

   while(true)
   {
    try
    {
     sb.setLength(0);
     ft.getFormatter().exportGroup(grp, auxInf, sb, false);
     break;
    }
    catch(Exception e)
    {
     restart++;

     stat.incRecoverAttempt();

     if(restart > MaxErrorRecoverAttempts)
      throw e;
    }

   }
   
   putIntoQueue(ft.getGroupQueue(), sb.toString());
  }
  
  return needMoreData;
 }
 
 private boolean checkStopFlag()
 {
  if(stopFlag.get())
  {
   log.debug("({}) Stop flag set. Sending FINISH message", Thread.currentThread().getName());
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));
   return true;
  }
  
  return false;
 }
 
 private boolean checkTTLexpired()
 {
  if(maxObjsPerThrSoft > 0 && objectCount > maxObjsPerThrSoft - sgQM.getChunkSize() )
  {
   log.debug("({}) Thread TTL expired. Processed {} objects. Sending TTL message", Thread.currentThread().getName(), objectCount);

   sgQM.close();

   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_TTL, this));
   return true;
  }
  
  return false;
 }
 
 @Override
 public void run()
 {
  procThread = Thread.currentThread();

  procThread.setName(procThread.getName() + "-ExporterTask-gen" + (++genNo) + "-lane" + laneNo);

  AuxInfo auxInf = null;

  if(myEqFactory != null)
   auxInf = new AuxInfoImpl(myEqFactory);


  objectCount = 0;
  
  try
  {
   StringBuilder sb = new StringBuilder();

   boolean needMoreData = true;
   
   groupLoop: while(needGroupLoop)
   {
    if(checkStopFlag()  || checkTTLexpired() )
     return;

    Collection<BioSampleGroup> grps = null;

    grps = sgQM.getGroups();

    if(grps.size() == 0)
    {
     log.debug("({}) No more groups to process", Thread.currentThread().getName());
     needGroupLoop = false;
    }

    for(BioSampleGroup grp : grps)
    {
     if( checkStopFlag() )
      return;
      
     objectCount++;

     stat.incGroupCounter();

     boolean grpPub = AbstractXMLFormatter.isGroupPublic(grp, stat.getNowDate());

     if(grpPub)
      stat.incGroupPublicCounter();

     needMoreData = formatGroup(grp, auxInf, sb);

     if(!needMoreData)
      break groupLoop;


     for(BioSample s : grp.getSamples())
     {
      if( checkStopFlag() )
       return;
      
      if(maxObjsPerThrHard > 0 && objectCount >= maxObjsPerThrHard )
       break;

      if(!sgQM.checkInSample(s.getId()))
       continue;

      objectCount++;

      countSample(s);

      needMoreData = formatSample(s, auxInf, sb);

      if(!needMoreData)
       break groupLoop;
     }

    }

    if(checkStopFlag() || checkTTLexpired() )
     return;


    if(auxInf != null)
     auxInf.clear();

   }

   
   
   sampleLoop: while( needMoreData )
   {

    Collection<BioSample> smpls = null;

    smpls = sgQM.getSamples();

    if(smpls.size() == 0)
    {
     log.debug("({}) No more data to process", Thread.currentThread().getName());
     break;
    }

    for(BioSample s : smpls)
    {
     if( checkStopFlag() )
      return;

     if(!sgQM.checkInSample(s.getId()))
      continue;

     objectCount++;

     countSample(s);

     needMoreData = formatSample(s, auxInf, sb);

     if(!needMoreData)
      break sampleLoop;

    }

    if(checkStopFlag() || checkTTLexpired() )
     return;

    if(auxInf != null)
     auxInf.clear();

   }
   
   if(!needMoreData)
    log.debug("({}) Output tasks don't need more data.", Thread.currentThread().getName());

   stat.addRecoverAttempt( sgQM.getRecovers() );
   
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