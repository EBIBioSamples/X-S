package uk.ac.ebi.biosd.xs.output;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;

public interface OutputModule
{

 public XMLFormatter getFormatter();

 public Appendable getGroupOut();

 public Appendable getSampleOut();
 
 public boolean isGroupedSamplesOnly();
 
 public void finish();
}
