package uk.ac.ebi.biosd.xs.service.mtexport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ebi.biosd.xs.util.Counter;

public class MTExporterStat
{
 private int groupCount=0;
 private int sampleCount=0;
 private  int uniqSampleCount=0;
 
 private final Set<String> sampleSet = new HashSet<>();
 private final Map<String, Counter> srcMap = new HashMap<String, Counter>();
 
 public void reset()
 {
  groupCount=0;
  sampleCount=0;
  uniqSampleCount=0;
  
  sampleSet.clear();
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

}
