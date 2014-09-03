package uk.ac.ebi.biosd.xs.mtexport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.StringUtils;
import uk.ac.ebi.biosd.xs.util.collection.LongHashSet;
import uk.ac.ebi.biosd.xs.util.collection.LongSet;

public class ExporterStat
{
 private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 
 private int msiCount=0;
 private int groupCount=0;
 private int sampleCount=0;
 private int groupPublicCount=0;
 private int samplePublicUniqCount=0;
 private  int uniqSampleCount=0;
 private final Date now;
 private int threads;
 
// private final Set<String> sampleSet = new HashSet<>();
 
 private final LongSet sampleSet = new LongHashSet();
 private final LongSet groupSet = new LongHashSet();
 
 private final Map<String, Counter> srcNmMap = new HashMap<String, Counter>();
 private final Map<String, Counter> srcAccMap = new HashMap<String, Counter>();
 
 public ExporterStat( Date now )
 {
  this.now = now;
 }
 
 public void reset()
 {
  groupCount=0;
  sampleCount=0;
  uniqSampleCount=0;
  
  sampleSet.clear();
  groupSet.clear();
 }
 
 public Date getNowDate()
 {
  return now;
 }
 
 public synchronized void addGroupCounter( int n )
 {
  groupCount+=n;
 }
 
 public synchronized void addSampleCounter( int n )
 {
  sampleCount+=n;
 }
 
 public synchronized void addUniqSampleCounter( int n )
 {
  uniqSampleCount+=n;
 }


 public synchronized void incGroupCounter()
 {
  groupCount++;
 }
 
 public synchronized void incSampleCounter()
 {
  sampleCount++;
 }

 public synchronized void incUniqSampleCounter()
 {
  uniqSampleCount++;
 }

 public int getGroupCount()
 {
  return groupCount;
 }

 public int getSampleCount()
 {
  return sampleCount;
 }
 
 public int getMSICount()
 {
  return msiCount;
 }


 public int getUniqSampleCount()
 {
  return uniqSampleCount;
 }
 
// public synchronized boolean addSample( String id )
// {
//  return sampleSet.add(id);
// }
// 
// public synchronized boolean containsSample( String id )
// {
//  return sampleSet.contains(id);
// }
 
 public synchronized boolean addSample( long id )
 {
  return sampleSet.add(id);
 }

 public synchronized boolean containsSample( long id )
 {
  return sampleSet.contains(id);
 }
 
 public synchronized boolean addGroup( long id )
 {
  return groupSet.add(id);
 }

 public synchronized boolean containsGroup( long id )
 {
  return groupSet.contains(id);
 }

 
 public synchronized void addToSourceByName( String srcName, int cnt )
 {
  Counter c = srcNmMap.get(srcName);

  if(c == null)
   srcNmMap.put(srcName, new Counter(cnt));
  else
   c.add(cnt);
 }
 
 public Map<String, Counter> getSourcesByNameMap()
 {
  return srcNmMap;
 }
 
 public synchronized void addToSourceByAcc( String srcName, int cnt )
 {
  Counter c = srcAccMap.get(srcName);

  if(c == null)
   srcAccMap.put(srcName, new Counter(cnt));
  else
   c.add(cnt);
 }
 
 public Map<String, Counter> getSourcesByAccMap()
 {
  return srcAccMap;
 }

 public int getGroupPublicCount()
 {
  return groupPublicCount;
 }

 public int getSamplePublicUniqCount()
 {
  return samplePublicUniqCount;
 }
 
 public synchronized void incGroupPublicCounter()
 {
  groupPublicCount++;
 }

 public synchronized void incMSICounter()
 {
  msiCount++;
 }
 
 public synchronized void incSamplePublicUniqCounter()
 {
  samplePublicUniqCount++;
 }

 public String createReport(Date startTime, Date endTime, int threads)
 {
  long startTs = startTime.getTime();
  long endTs = endTime.getTime();
  
  long rate = getMSICount()!=0? (endTs-startTs)/getMSICount():0;
//  long grpRate = getGroupCount()!=0? (endTs-startTs)/getGroupCount():0;
//  long smpRate = getSampleCount()!=0? (endTs-startTs)/getSampleCount():0;
  
  StringBuffer summaryBuf = new StringBuffer();

  summaryBuf.append("\n<!-- Exported: ").append(getMSICount()).append(" MSIs in ").append(threads).append(" threads. Rate: ").append(rate).append("ms per msi -->");

  rate = getGroupCount()!=0? (endTs-startTs)/getGroupCount():0;
  summaryBuf.append("\n<!-- All groups: ").append(getGroupCount()).append(". Rate: ").append(rate).append("ms per group -->");
  summaryBuf.append("\n<!-- Public groups: ").append(getGroupPublicCount()).append(" -->");
  
  rate = getSampleCount()!=0? (endTs-startTs)/getSampleCount():0;
  summaryBuf.append("\n<!-- Samples in groups: ").append(getSampleCount()).append(". Rate: ").append(rate).append("ms per sample -->");
  
  rate = getUniqSampleCount()!=0? (endTs-startTs)/getUniqSampleCount():0;
  summaryBuf.append("\n<!-- Unique samples: ").append(getUniqSampleCount()).append(". Rate: ").append(rate).append("ms per unique sample -->");

  summaryBuf.append("\n<!-- Public unique samples: ").append(getSamplePublicUniqCount()).append(" -->");
  
  summaryBuf.append("\n<!-- Start time: ").append(simpleDateFormat.format(startTime)).append(" -->");
  summaryBuf.append("\n<!-- End time: ").append(simpleDateFormat.format(endTime)).append(". Time spent: "+StringUtils.millisToString(endTs-startTs)).append(" -->");
  summaryBuf.append("\n<!-- Thank you. Good bye. -->\n");
  
  return summaryBuf.toString();

 }

 public int getThreads()
 {
  return threads;
 }

 public void setThreads(int threads)
 {
  this.threads = threads;
 }

}
