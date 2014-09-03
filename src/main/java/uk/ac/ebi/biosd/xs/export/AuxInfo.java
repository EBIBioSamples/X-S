package uk.ac.ebi.biosd.xs.export;

import java.util.Collection;

public interface AuxInfo
{

 void clear();
 void destroy();

 Collection<EquivalenceRecord> getSampleEquivalences(String acc);
 Collection<EquivalenceRecord> getGroupEquivalences(String acc);

}
