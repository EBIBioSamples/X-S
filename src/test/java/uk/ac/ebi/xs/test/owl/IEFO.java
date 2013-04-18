package uk.ac.ebi.xs.test.owl;

import java.util.Map;
import java.util.Set;

public interface IEFO
{
    public final static int INCLUDE_SELF = 1;
    public final static int INCLUDE_ALT_TERMS = 2;
    public final static int INCLUDE_CHILD_TERMS = 4;
    public final static int INCLUDE_CHILD_ALT_TERMS = 8;
    public final static int INCLUDE_PART_OF_TERMS = 16;

    public final static int INCLUDE_ALL =
            INCLUDE_SELF
                    + INCLUDE_ALT_TERMS
                    + INCLUDE_CHILD_TERMS
                    + INCLUDE_CHILD_ALT_TERMS
                    + INCLUDE_PART_OF_TERMS;

    public final static int INCLUDE_CHILDREN =
            INCLUDE_CHILD_TERMS
                    + INCLUDE_CHILD_ALT_TERMS
                    + INCLUDE_PART_OF_TERMS;

    public final static String ROOT_ID = "http://www.ebi.ac.uk/efo/EFO_0000001";

    public Map<String, EFONode> getMap();

    public Map<String, Set<String>> getPartOfIdMap();

    public Set<String> getTerms( String efoId, int includeFlags );

    public String getVersionInfo();
}