package uk.ac.ebi.biosd.xs.output.ebeye;

import java.io.IOException;
import java.util.Map;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;
import uk.ac.ebi.biosd.xs.output.OutputModule;

public class EBEyeOutputModule implements OutputModule
{

 public EBEyeOutputModule(String key, Map<String, String> cfg)
 {
  // TODO Auto-generated constructor stub
 }

 @Override
 public XMLFormatter getFormatter()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public Appendable getGroupOut()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public Appendable getSampleOut()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public boolean isGroupedSamplesOnly()
 {
  // TODO Auto-generated method stub
  return false;
 }

 @Override
 public boolean isSourcesByAcc()
 {
  // TODO Auto-generated method stub
  return false;
 }

 @Override
 public boolean isSourcesByName()
 {
  // TODO Auto-generated method stub
  return false;
 }

 @Override
 public void start() throws IOException
 {
  // TODO Auto-generated method stub
  
 }

 @Override
 public void finish(ExporterStat stat) throws IOException
 {
  // TODO Auto-generated method stub
  
 }

 @Override
 public void cancel() throws IOException
 {
  // TODO Auto-generated method stub
  
 }

}
