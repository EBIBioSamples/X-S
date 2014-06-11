package uk.ac.ebi.biosd.xs.mtexport;

public class MTTaskConfig
{
 private long    since;
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
