package uk.ac.ebi.biosd.xs.log;

public class TagStat
{
 private long summary=0;
 private long entry=0;
 private long checkpoint=0;
 private int nEntries=0;

 public long getSummary()
 {
  return summary;
 }

 public void setSummary(long summary)
 {
  this.summary = summary;
 }

 public long getEntry()
 {
  return entry;
 }

 public void setEntry(long entry)
 {
  this.entry = entry;
 }

 public long getCheckpoint()
 {
  return checkpoint;
 }

 public void setCheckpoint(long checkpoint)
 {
  this.checkpoint = checkpoint;
 }

 public int getNEntries()
 {
  return nEntries;
 }

 public void setNEntries(int nEntries)
 {
  this.nEntries = nEntries;
 }
}
