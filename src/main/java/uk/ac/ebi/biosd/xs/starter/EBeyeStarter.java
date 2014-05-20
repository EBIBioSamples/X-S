package uk.ac.ebi.biosd.xs.starter;

import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.ebeye.EBeyeExport;
import uk.ac.ebi.biosd.xs.util.ParamPool;

public class EBeyeStarter
{
 static final String LimitParameter = "limit";
 static final String GenGroupParameter = "generateGroups";
 static final String GenSamplesParameter = "generateSamples";
 static final String ExportPrivateParameter = "exportPrivate";
 static final String ThreadsParameter = "threads";
 
 public static boolean start( ParamPool prms, String prmPfx, final StarterLog log )
 {
  final EBeyeExport exp = EBeyeExport.getInstance();

  if(exp == null)
  {
   log.sendErrorMsg("EBeyeExport was not initialized. See logs");
   return false;
  }

  int limit = -1;

  String limStr = prms.getParameter(prmPfx+LimitParameter);

  if(limStr != null)
  {
   try
   {
    limit = Integer.parseInt(limStr);
   }
   catch(Exception e)
   {
    log.sendErrorMsg("Invalid value of '" + prmPfx+LimitParameter + "' parameter");
    return false;
   }
  }

  String genSmpStr = prms.getParameter(prmPfx+GenSamplesParameter);

  final boolean genSmp = genSmpStr == null ? true : "1".equals(genSmpStr) || "yes".equalsIgnoreCase(genSmpStr) || "on".equalsIgnoreCase(genSmpStr)
    || "true".equalsIgnoreCase(genSmpStr);

  String expPrvStr = prms.getParameter(prmPfx+ExportPrivateParameter);

  final boolean expPrv = expPrvStr == null ? false : "1".equals(expPrvStr) || "yes".equalsIgnoreCase(expPrvStr) || "on".equalsIgnoreCase(expPrvStr)
    || "true".equalsIgnoreCase(expPrvStr);

  String prm = prms.getParameter(prmPfx+ThreadsParameter);

  int nThrs = -1;

  if(prm != null)
  {
   try
   {
    nThrs = Integer.parseInt(prm);
   }
   catch(Exception e)
   {
   }
  }

  if(exp.isBusy())
  {
   log.sendErrorMsg("EBEye export is busy");
   return false;
  }

  final int fLimit = limit;
  final int fThreads = nThrs;

  new Thread(new Runnable()
  {

   @Override
   public void run()
   {
    try
    {
     exp.export(fLimit, genSmp, !expPrv, fThreads);
    }
    catch(Throwable e)
    {
     LoggerFactory.getLogger(EBeyeStarter.class).error("Export error: "+(e.getMessage()!=null?e.getMessage():e.getClass().getName()));
    }
   }
  }, "Manual EBeye export").start();

  log.sendInfoMsg("EBEye export has been initiated");

  return true;
 }

}
