package uk.ac.ebi.biosd.xs.mtexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
 
 private File expLogDir;
 private PrintWriter expLog;

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

  expLogDir = tCfg.getThreadLogDir();
  
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

  try
  {
   if(expLog != null)
    expLog.close();

   if(expLogDir != null)
    expLog = new PrintWriter(new FileOutputStream(new File(expLogDir, procThread.getName() + ".log"), true));

  }
  catch(Exception e)
  {
   e.printStackTrace();
  }

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
    if(checkStopFlag() )
    {
     assert explog("Stop flag");
     return;
    }
    
    if(checkTTLexpired() )
    {
     assert explog("277 Thread TTL expired");
     return;
    }

    Collection<BioSampleGroup> grps = null;

    grps = sgQM.getGroups();

    if(grps.size() == 0)
    {
     log.debug("({}) No more groups to process", Thread.currentThread().getName());
     needGroupLoop = false;
     assert explog("289 No more groups");
    }

    for(BioSampleGroup grp : grps)
    {
     assert explog("Group: "+grp.getId()+" "+grp.getAcc());
     
     if( checkStopFlag() )
     {
      assert explog("298 Stop flag");
      return;
     }
     
     objectCount++;

     stat.incGroupCounter();

     boolean grpPub = AbstractXMLFormatter.isGroupPublic(grp, stat.getNowDate());

     if(grpPub)
      stat.incGroupPublicCounter();
     
     assert explog("311 Group is "+(grpPub?"":"NOT ")+"public");

     
     needMoreData = formatGroup(grp, auxInf, sb);

     assert explog("316 Group has been processed: "+sb.length()+" chars");
     
     if(!needMoreData)
     {
      assert explog("320 Outputs don't need more data");
      break groupLoop;
     }
     
     for(BioSample s : grp.getSamples())
     {
      assert explog("326 In group sample "+s.getId()+" "+s.getAcc());

      if( checkStopFlag() )
      {
       assert explog("330 Stop flag");
       return;
      }
       
      
      if(maxObjsPerThrHard > 0 && objectCount >= maxObjsPerThrHard )
      {
       assert explog("337 Hard limit exceeded: "+maxObjsPerThrHard);
       break;
      }
      
      if(!sgQM.checkInSample(s.getId()))
      {
       assert explog("343 Sample has been checked in already");
       continue;
      }
      
      objectCount++;

      countSample(s);

      assert explog("351 Sample public: "+reportSamplePublic(s) );

      needMoreData = formatSample(s, auxInf, sb);

      assert explog("355 Sample has been processed: "+sb.length()+" chars");
     
      if(!needMoreData)
      {
       assert explog("359 Outputs don't need more data");
       break groupLoop;
      }
     }

    }

    if(checkStopFlag() )
    {
     assert explog("368 Stop flag");
     return;
    }
    
    if(checkTTLexpired() )
    {
     assert explog("374 Thread TTL expired");
     return;
    }


    if(auxInf != null)
     auxInf.clear();

   }

   
   sampleLoop: while( needMoreData )
   {

    Collection<BioSample> smpls = null;

    smpls = sgQM.getSamples();

    if(smpls.size() == 0)
    {
     assert explog("394 No more samples");
     log.debug("({}) No more data to process", Thread.currentThread().getName());
     break;
    }

    for(BioSample s : smpls)
    {
     assert explog("401 Out of group sample "+s.getId()+" "+s.getAcc());

     if( checkStopFlag() )
     {
      assert explog("405 Stop flag");
      return;
     }
     

     if(!sgQM.checkInSample(s.getId()))
     {
      assert explog("412 Sample has been checked in already");
      continue;
     }
     
     objectCount++;

     countSample(s);

     assert explog("420 Sample public: "+reportSamplePublic(s));

     needMoreData = formatSample(s, auxInf, sb);

     assert explog("424 Sample has been processed: "+sb.length()+" chars");

     if(!needMoreData)
     {
      assert explog("428 Outputs don't need more data");
      break sampleLoop;
     }
    }

    if(checkStopFlag() )
    {
     assert explog("435 Stop flag");
     return;
    }
    
    if(checkTTLexpired() )
    {
     assert explog("441 Thread TTL expired");
     return;
    }

    if(auxInf != null)
     auxInf.clear();

   }
   
   if(!needMoreData)
   {
    assert explog("452 Outputs don't need more data");
    log.debug("({}) Output tasks don't need more data.", Thread.currentThread().getName());
   }
   
   stat.addRecoverAttempt( sgQM.getRecovers() );
   
   log.debug("({}) Thread terminating. Sending FINISH message", Thread.currentThread().getName());
   putIntoQueue(controlQueue, new ControlMessage(Type.PROCESS_FINISH, this));

   assert explog("461 Finishing thread");

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
   
   if( expLog !=null )
   {
    try
    {
     expLog.close();
     expLog = null;
    }
    catch(Exception e2)
    {
     e2.printStackTrace();
    }
   }
  }

 }
  
 private String reportSamplePublic( BioSample smp )
 {
  boolean pub = AbstractXMLFormatter.isSamplePublic(smp, stat.getNowDate());
  
  if( pub )
   return "yes";
  
  StringBuilder sb = new StringBuilder();
  
  sb.append(" no PubFlg: ");
  
  if( smp.getPublicFlag() != null )
   sb.append(smp.getPublicFlag());
  else
   sb.append("null");
  
  sb.append(" RDt: ");
  
  if( smp.getReleaseDate() != null )
   sb.append(smp.getReleaseDate()).append(" (now: ").append(stat.getNowDate()).append(")");
  else
   sb.append("null");
  
  if( smp.getMSIs() == null )
  {
   sb.append(" MSIs: none");
   return sb.toString();
  }
  
  for( MSI msi : smp.getMSIs() )
  {
   sb.append(" MSI: ").append(msi.getAcc());

   sb.append(" PubFlg: ");
   
   if( msi.getPublicFlag() != null )
    sb.append(msi.getPublicFlag());
   else
    sb.append("null");

   sb.append(" RDt: ");
   
   if( msi.getReleaseDate() != null )
    sb.append(msi.getReleaseDate()).append(" (now: ").append(stat.getNowDate()).append(")");
   else
    sb.append("null");
  }

  return sb.toString();
 }
 
 private boolean explog(String msg)
 {
  if( expLog != null )
   expLog.println(msg);
  
  return true;
 }
 
 private void countSample( BioSample s )
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