package uk.ac.ebi.biosd.xs.mtexport;

import java.util.List;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;

public interface QueryManager
{
 List<BioSampleGroup> getGroups();

 List<BioSample> getSamples();

 List<MSI> getMSIs();
 
 int getChunkSize();
 
 int getRecovers();
 
 void release();

 void close();
}
