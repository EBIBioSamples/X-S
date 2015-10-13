package uk.ac.ebi.biosd.xs.mtexport;

import java.io.File;

public class MTTaskConfig
{
 private long    since;
 private Double  groupMultiplier;
 private Double  sampleMultiplier;
 private int     maxItemsPerThread=-1;
 private int     maxItemsPerThreadSoft=-1;
 private File    threadLog;

 public int getItemsPerThreadHardLimit()
 {
  return maxItemsPerThread;
 }
 
 public int getItemsPerThreadSoftLimit()
 {
  return maxItemsPerThreadSoft;
 }

 public void setItemsPerThreadHardLimit(int maxItemsPerTread)
 {
  this.maxItemsPerThread = maxItemsPerTread;
 }

 public void setItemsPerThreadSoftLimit(int maxItemsPerTread)
 {
  this.maxItemsPerThreadSoft = maxItemsPerTread;
 }

 public long getSince()
 {
  return since;
 }

 public void setSince(long since)
 {
  this.since = since;
 }



 public Double getGroupMultiplier()
 {
  return groupMultiplier;
 }

 public void setGroupMultiplier(Double groupMultiplier)
 {
  this.groupMultiplier = groupMultiplier;
 }

 public Double getSampleMultiplier()
 {
  return sampleMultiplier;
 }

 public void setSampleMultiplier(Double sampleMultiplier)
 {
  this.sampleMultiplier = sampleMultiplier;
 }

 public File getThreadLogDir()
 {
  return threadLog;
 }

 public void setThreadLogDir(File threadLog)
 {
  this.threadLog = threadLog;
 }
}
