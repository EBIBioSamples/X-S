package uk.ac.ebi.biosd.xs.export;

import uk.ac.ebi.fg.myequivalents.utils.EntityIdResolver;

public class EquivalenceRecord
{
// public static final String placeHolder = "${accession}";
 
 private String accession;
 private String title;
 private String url;

 public EquivalenceRecord(String accession, String title, String uriPattern )
 {
  this.accession = accession;
  this.title = title;
  url = EntityIdResolver.buildUriFromAcc(accession, uriPattern);

 }

 public String getAccession()
 {
  return accession;
 }

 public void setAccession(String accession)
 {
  this.accession = accession;
 }

 public String getTitle()
 {
  return title;
 }

 public void setTitle(String title)
 {
  this.title = title;
 }

 public String getUrl()
 {
  return url;
 }

 public void setUrl(String url)
 {
  this.url = url;
 }

}
