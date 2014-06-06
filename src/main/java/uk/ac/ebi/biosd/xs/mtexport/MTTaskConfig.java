package uk.ac.ebi.biosd.xs.mtexport;

public class MTTaskConfig
{
 private long    since;
 private boolean sourcesByName;
 private boolean groupedSamplesOnly;
 private Double  groupMultiplier;
 private Double  sampleMultiplier;

 public long getSince()
 {
  return since;
 }

 public void setSince(long since)
 {
  this.since = since;
 }

 public boolean isSourcesByName()
 {
  return sourcesByName;
 }

 public void setSourcesByName(boolean sourcesByName)
 {
  this.sourcesByName = sourcesByName;
 }

 public boolean isGroupedSamplesOnly()
 {
  return groupedSamplesOnly;
 }

 public void setGroupedSamplesOnly(boolean groupedSamplesOnly)
 {
  this.groupedSamplesOnly = groupedSamplesOnly;
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
}
