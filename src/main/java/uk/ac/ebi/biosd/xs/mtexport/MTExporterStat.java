package uk.ac.ebi.biosd.xs.mtexport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.StringUtils;

public class MTExporterStat
{
 private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 
 private int groupCount=0;
 private int sampleCount=0;
 private int groupPublicCount=0;
 private int samplePublicUniqCount=0;
 private  int uniqSampleCount=0;
 private final Date now;
 
 private final Set<String> sampleSet = new HashSet<>();
 private final Map<String, Counter> srcMap = new HashMap<String, Counter>();
 
 public MTExporterStat( Date now )
 {
  this.now = now;
 }
 
 public void reset()
 {
  groupCount=0;
  sampleCount=0;
  uniqSampleCount=0;
  
  sampleSet.clear();
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

 public int getUniqSampleCount()
 {
  return uniqSampleCount;
 }
 
 public synchronized boolean addSample( String id )
 {
  return sampleSet.add(id);
 }
 
 public synchronized boolean containsSample( String id )
 {
  return sampleSet.contains(id);
 }
 
 public synchronized void addToSource( String srcName, int cnt )
 {
  Counter c = srcMap.get(srcName);

  if(c == null)
   srcMap.put(srcName, new Counter(cnt));
  else
   c.add(cnt);
 }
 
 public Map<String, Counter> getSourcesMap()
 {
  return srcMap;
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
 
 public synchronized void incSamplePublicUniqCounter()
 {
  samplePublicUniqCount++;
 }

 public String createReport(Date startTime, Date endTime, int threads)
 {
  long startTs = startTime.getTime();
  long endTs = endTime.getTime();
  
  long rate = getGroupCount()!=0? (endTs-startTs)/getGroupCount():0;
  
  StringBuffer summaryBuf = new StringBuffer();

  summaryBuf.append("\n<!-- Exported: ").append(getGroupCount()).append(" groups in ").append(threads).append(" threads. Rate: ").append(rate).append("ms per group -->");
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

}
